package org.example.project.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * Mints HS256 access tokens with the same claims [JwtAuthPluginInstaller]'s verifier pins: issuer,
 * audience, `sub` = account UUID, and an expiry of [JwtConfig.accessTokenTtl] from now.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class JwtAccessTokenIssuer(private val config: JwtConfig) : AccessTokenIssuer {
    private val algorithm = Algorithm.HMAC256(config.secret)

    override fun issue(accountId: AccountId): AccessToken {
        val now = Clock.System.now()
        val expiresAt = now + config.accessTokenTtl
        val token =
            JWT.create()
                .withIssuer(config.issuer)
                .withAudience(config.audience)
                .withSubject(accountId.value.toString())
                .withIssuedAt(now.toJavaInstant())
                .withExpiresAt(expiresAt.toJavaInstant())
                .sign(algorithm)
        return AccessToken(token = token, expiresAt = expiresAt)
    }
}
