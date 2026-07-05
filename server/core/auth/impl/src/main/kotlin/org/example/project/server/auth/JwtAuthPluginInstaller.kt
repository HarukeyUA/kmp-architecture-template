package org.example.project.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import java.util.UUID
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/**
 * Installs the `access-jwt` provider that backs [authenticatedRoutes] (ADR-0009 as amended): the
 * bearer access token is verified **statelessly** — HS256 signature, pinned issuer/audience, and
 * expiry — and its `sub` claim becomes the [Principal]. No session-store lookup happens here;
 * revocation bites on the refresh path, within [JwtConfig.accessTokenTtl].
 */
@Inject
@ContributesIntoSet(AppScope::class)
class JwtAuthPluginInstaller(private val config: JwtConfig) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.AUTHENTICATION

    override fun Application.install() {
        install(Authentication) {
            jwt(ACCESS_AUTH) {
                verifier(
                    JWT.require(Algorithm.HMAC256(config.secret))
                        .withIssuer(config.issuer)
                        .withAudience(config.audience)
                        .build()
                )
                validate { credential ->
                    // A non-UUID subject can only be a token we didn't mint (or a signing-key
                    // cross-use); treat it as unauthenticated rather than a 500.
                    credential.subject
                        ?.let { subject -> runCatching { UUID.fromString(subject) }.getOrNull() }
                        ?.let { accountId -> Principal(AccountId(accountId)) }
                }
            }
        }
    }
}
