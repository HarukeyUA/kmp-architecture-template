package org.example.project.server.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.util.UUID
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest

class SessionCacheTest {
    @Test
    fun `cached session stops resolving at session expiry before cache ttl`() = runTest {
        var now = Clock.System.now()
        val cache = CaffeineSessionCache(ttl = 5.seconds, now = { now })
        val principal = Principal(AccountId(UUID.randomUUID()))
        var loads = 0

        val first =
            cache.resolve(TOKEN_HASH) {
                loads += 1
                CachedSession(principal, now + 25.milliseconds)
            }

        assertThat(first).isEqualTo(principal)

        now += 80.milliseconds

        val second =
            cache.resolve(TOKEN_HASH) {
                loads += 1
                CachedSession(principal, now + 25.milliseconds)
            }

        assertThat(second).isNull()
        assertThat(loads).isEqualTo(1)
    }

    @Test
    fun `forged-token spray cannot evict a live session`() = runTest {
        val now = Clock.System.now()
        // Tiny bounds so the spray vastly exceeds both caches' capacity.
        val cache =
            CaffeineSessionCache(ttl = 60.seconds, maxSize = 4, absentMaxSize = 4, now = { now })
        val principal = Principal(AccountId(UUID.randomUUID()))
        var loads = 0
        val loader: suspend (String) -> CachedSession? = {
            loads += 1
            CachedSession(principal, now + 5.minutes)
        }

        assertThat(cache.resolve(TOKEN_HASH, loader)).isEqualTo(principal)

        // Unauthenticated junk lands in the separate absent cache, never competing with
        // Present entries for space — under the old shared cache this evicted live sessions.
        repeat(1_000) { i -> cache.resolve("forged-$i") { null } }

        assertThat(cache.resolve(TOKEN_HASH, loader)).isEqualTo(principal)
        assertThat(loads).isEqualTo(1)
    }

    @Test
    fun `a repeated unknown token hits the loader once`() = runTest {
        val now = Clock.System.now()
        val cache = CaffeineSessionCache(ttl = 60.seconds, now = { now })
        var loads = 0

        repeat(3) {
            assertThat(
                    cache.resolve(TOKEN_HASH) {
                        loads += 1
                        null
                    }
                )
                .isNull()
        }

        assertThat(loads).isEqualTo(1)
    }

    @Test
    fun `revoke during an in-flight load cannot resurrect the session`() = runTest {
        val now = Clock.System.now()
        val cache = CaffeineSessionCache(ttl = 60.seconds, now = { now })
        val principal = Principal(AccountId(UUID.randomUUID()))
        val loadStarted = CompletableDeferred<Unit>()
        val revokeDone = CompletableDeferred<Unit>()

        val inFlight = async {
            cache.resolve(TOKEN_HASH) {
                loadStarted.complete(Unit)
                revokeDone.await()
                CachedSession(principal, now + 5.minutes)
            }
        }
        loadStarted.await()
        // The tombstone lands while the DB load is still in flight; the load's publish must
        // not overwrite it (set-after-delete via the per-key compute lock).
        cache.invalidate(TOKEN_HASH)
        revokeDone.complete(Unit)

        assertThat(inFlight.await()).isNull()

        // The tombstone survived: a later resolve short-circuits without touching the DB.
        var reloads = 0
        assertThat(
                cache.resolve(TOKEN_HASH) {
                    reloads += 1
                    null
                }
            )
            .isNull()
        assertThat(reloads).isEqualTo(0)
    }

    private companion object {
        const val TOKEN_HASH = "token-hash"
    }
}
