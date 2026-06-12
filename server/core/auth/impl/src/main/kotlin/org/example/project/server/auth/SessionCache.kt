package org.example.project.server.auth

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaDuration

/**
 * Per-instance cache in front of the session table so resolving a token digest on every
 * authenticated request isn't a DB round-trip. Behind an interface so a shared backing (Redis) can
 * replace it without touching call sites (ADR-0010).
 *
 * Freshness model: [invalidate] writes a tombstone, so a revoked session stops resolving
 * immediately on the current node — and because that write serializes with an in-flight
 * read-through publish, a concurrent load can't resurrect it (set-after-delete). Other nodes never
 * see the tombstone, so a session already cached there stays ≤TTL stale until its entry expires
 * (ADR-0010 bounded staleness).
 */
interface SessionCache {
    suspend fun resolve(tokenHash: String, loader: suspend (String) -> CachedSession?): Principal?

    fun invalidate(tokenHash: String)
}

data class CachedSession(val principal: Principal, val expiresAt: Instant)

/**
 * Two Caffeine caches split by **trust level**: [cache] holds only entries tied to real sessions
 * ([Lookup.Present] and [Lookup.Revoked] tombstones — both bounded by legitimate activity), while
 * unknown-token misses go to the separate, smaller [absent] cache. Unauthenticated input must never
 * compete with authenticated state for cache space: with a shared cache, an attacker spraying
 * fabricated bearer tokens could evict every live session and turn each real request into a DB
 * round-trip.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CaffeineSessionCache(
    ttl: Duration = DEFAULT_TTL,
    maxSize: Long = DEFAULT_MAX_SIZE,
    absentMaxSize: Long = DEFAULT_ABSENT_MAX_SIZE,
    private val now: () -> Instant = Clock.System::now,
) : SessionCache {
    private val cache: Cache<String, Lookup> =
        Caffeine.newBuilder().expireAfterWrite(ttl.toJavaDuration()).maximumSize(maxSize).build()

    private val absent: Cache<String, Unit> =
        Caffeine.newBuilder()
            .expireAfterWrite(ttl.toJavaDuration())
            .maximumSize(absentMaxSize)
            .build()

    override suspend fun resolve(
        tokenHash: String,
        loader: suspend (String) -> CachedSession?,
    ): Principal? {
        cache.getIfPresent(tokenHash)?.let { cached ->
            return when (cached) {
                Lookup.Revoked -> null
                is Lookup.Present -> cached.session.takeIfFresh(tokenHash)?.principal
            }
        }
        if (absent.getIfPresent(tokenHash) != null) return null
        val loaded = loader(tokenHash)?.takeIf { it.isFresh() }
        if (loaded == null) {
            // Remember the miss in the junk cache only. This shields the DB from a client
            // re-sending the same dead token; a spray of *fabricated* tokens never repeats a key,
            // so it gains nothing from any cache — here it can merely churn other junk. (A racing
            // [invalidate] tombstone is not overwritten by this: it lives in [cache], and both
            // entries independently resolve to null — fail closed either way.)
            absent.put(tokenHash, Unit)
            return null
        }
        // Publish atomically: this `compute` and [invalidate]'s tombstone write serialize on the
        // per-key lock, so a load that began before a concurrent revoke can't overwrite the
        // tombstone. A surviving tombstone wins and this resolve fails closed (null).
        val published =
            cache.asMap().compute(tokenHash) { _, existing ->
                existing as? Lookup.Revoked ?: Lookup.Present(loaded)
            }
        return (published as? Lookup.Present)?.session?.takeIfFresh(tokenHash)?.principal
    }

    override fun invalidate(tokenHash: String) {
        // Tombstone rather than evict: a concurrent in-flight load would otherwise re-insert the
        // just-revoked principal after a plain eviction. The tombstone expires with the normal TTL,
        // which is harmless since a token hash is never reissued.
        cache.put(tokenHash, Lookup.Revoked)
    }

    private sealed interface Lookup {
        data object Revoked : Lookup

        data class Present(val session: CachedSession) : Lookup
    }

    private fun CachedSession.takeIfFresh(tokenHash: String): CachedSession? {
        if (isFresh()) return this
        cache.invalidate(tokenHash)
        return null
    }

    private fun CachedSession.isFresh(): Boolean = expiresAt > now()

    private companion object {
        val DEFAULT_TTL: Duration = 60.seconds
        const val DEFAULT_MAX_SIZE: Long = 10_000L

        /** Sizing barely matters — junk evicting junk is fine; it only has to stay bounded. */
        const val DEFAULT_ABSENT_MAX_SIZE: Long = 1_000L
    }
}
