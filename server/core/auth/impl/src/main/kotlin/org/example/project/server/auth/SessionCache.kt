package org.example.project.server.auth

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Per-instance cache in front of the session table so resolving a token on every authenticated
 * request isn't a DB round-trip. Behind an interface so a shared backing (Redis) can replace it
 * without touching call sites (ADR-0010).
 *
 * Freshness model: a revoked session keeps resolving for at most the TTL; [invalidate] closes that
 * window to zero on the current node (effectively immediate single-node; ≤TTL stale on other
 * nodes).
 */
interface SessionCache {
    suspend fun resolve(token: String, loader: suspend (String) -> Principal?): Principal?

    fun invalidate(token: String)
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CaffeineSessionCache(ttl: Duration = DEFAULT_TTL, maxSize: Long = DEFAULT_MAX_SIZE) :
    SessionCache {
    private val cache: Cache<String, Lookup> =
        Caffeine.newBuilder().expireAfterWrite(ttl.toJavaDuration()).maximumSize(maxSize).build()

    override suspend fun resolve(
        token: String,
        loader: suspend (String) -> Principal?,
    ): Principal? {
        // Misses are cached as [Lookup.Absent] so a flood of fabricated tokens can't chain into a
        // flood of DB lookups.
        cache.getIfPresent(token)?.let { cached ->
            return (cached as? Lookup.Present)?.principal
        }
        val principal = loader(token)
        cache.put(token, principal?.let(Lookup::Present) ?: Lookup.Absent)
        return principal
    }

    override fun invalidate(token: String) {
        cache.invalidate(token)
    }

    private sealed interface Lookup {
        data object Absent : Lookup

        data class Present(val principal: Principal) : Lookup
    }

    private companion object {
        val DEFAULT_TTL: Duration = 60.seconds
        const val DEFAULT_MAX_SIZE: Long = 10_000L
    }
}
