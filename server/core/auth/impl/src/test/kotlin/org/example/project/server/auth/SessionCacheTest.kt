package org.example.project.server.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.util.UUID
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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

    private companion object {
        const val TOKEN_HASH = "token-hash"
    }
}
