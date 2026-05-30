package org.example.project.server.auth

import kotlin.time.Instant

/**
 * An opaque, server-validated session (CONTEXT.md): the bearer [token] proving [accountId], valid
 * until [expiresAt]. Revocation is a row delete; the client only ever holds the token.
 */
data class Session(val token: String, val accountId: AccountId, val expiresAt: Instant)
