package org.example.project.server.auth

import kotlin.time.Instant

/**
 * A short-lived, signed JWT proving an account until [expiresAt] (ADR-0009 as amended). Verified
 * statelessly by the auth middleware — never stored server-side, so it cannot be revoked
 * individually; revocation happens at the Session (refresh) layer and takes effect within
 * [JwtConfig.accessTokenTtl].
 */
data class AccessToken(val token: String, val expiresAt: Instant)

/**
 * Mints signed [AccessToken]s for an authenticated account. Part of the invariant auth infra: the
 * Credential module calls this on signup/login/refresh; the `:impl` middleware verifies the result.
 */
interface AccessTokenIssuer {
    fun issue(accountId: AccountId): AccessToken
}
