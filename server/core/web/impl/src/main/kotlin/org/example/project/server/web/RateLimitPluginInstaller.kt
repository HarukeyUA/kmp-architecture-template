package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

/**
 * Registers the strict per-client-IP tier for credential endpoints (ADR-0010 §11): pre-auth and
 * Argon2-expensive, they are the asymmetric-cost surface. There is deliberately **no** default tier
 * for other endpoints — they are cheap reads behind the session cache, and a per-node limit picked
 * without traffic data mostly punishes legitimate bursts. Per-node by design; a global limit needs
 * a shared backing (ADR-0010).
 *
 * Exhaustion responds through the StatusPages 429 hook as a typed `RateLimited(retryAfterSeconds)`
 * envelope, never Ktor's bare 429.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class RateLimitPluginInstaller(private val limits: WebLimitsConfig) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.RATE_LIMIT

    override fun Application.install() {
        install(RateLimit) {
            register(RateLimitName(CREDENTIAL_RATE_LIMIT_NAME)) {
                rateLimiter(limit = limits.credentialRateLimit, refillPeriod = 1.minutes)
                requestKey { call -> call.clientIp(limits.clientIpHeader) }
            }
        }
    }
}

/**
 * The rate-limit key: the configured trusted proxy header when present, otherwise the socket
 * address. The fallback matters — keying a proxied deployment on the socket would collapse every
 * client into the proxy's one bucket, and trusting a header on a directly-exposed box would let a
 * client mint a fresh bucket per request.
 */
private fun ApplicationCall.clientIp(clientIpHeader: String?): String =
    clientIpHeader?.let { request.headers[it] }?.takeIf { it.isNotBlank() }
        ?: request.origin.remoteHost
