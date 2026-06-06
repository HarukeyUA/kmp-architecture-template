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

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CaffeineSessionCache(
    ttl: Duration = DEFAULT_TTL,
    maxSize: Long = DEFAULT_MAX_SIZE,
    private val now: () -> Instant = Clock.System::now,
) : SessionCache {
    private val cache: Cache<String, Lookup> =
        Caffeine.newBuilder().expireAfterWrite(ttl.toJavaDuration()).maximumSize(maxSize).build()

    override suspend fun resolve(
        tokenHash: String,
        loader: suspend (String) -> CachedSession?,
    ): Principal? {
        // Misses are cached as [Lookup.Absent] so a flood of fabricated tokens can't chain into a
        // flood of DB lookups; a [Lookup.Revoked] tombstone short-circuits a revoked token with no
        // DB round-trip.
        cache.getIfPresent(tokenHash)?.let { cached ->
            return when (cached) {
                Lookup.Absent,
                Lookup.Revoked -> null
                is Lookup.Present -> cached.session.takeIfFresh(tokenHash)?.principal
            }
        }
        val loaded =
            loader(tokenHash)?.takeIf { it.isFresh() }?.let(Lookup::Present) ?: Lookup.Absent
        // Publish atomically: this `compute` and [invalidate]'s tombstone write serialize on the
        // per-key lock, so a load that began before a concurrent revoke can't overwrite the
        // tombstone. A surviving tombstone wins and this resolve fails closed (null).
        val published =
            cache.asMap().compute(tokenHash) { _, existing ->
                existing as? Lookup.Revoked ?: loaded
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
        data object Absent : Lookup

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
    }
}
