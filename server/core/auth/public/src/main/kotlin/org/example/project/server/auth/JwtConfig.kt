package org.example.project.server.auth

import kotlin.time.Duration

/**
 * Configuration for the stateless JWT access-token layer (ADR-0009 as amended): [secret] signs and
 * verifies HS256 access tokens, [issuer] and [audience] are pinned claims, and [accessTokenTtl] is
 * the revocation-staleness bound — a revoked account keeps working for at most this long on an
 * already-minted access token, because only the refresh path consults the session store.
 */
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenTtl: Duration,
)
