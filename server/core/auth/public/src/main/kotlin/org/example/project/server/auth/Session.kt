package org.example.project.server.auth

import kotlin.time.Instant

/**
 * An opaque, server-validated session: the [token] proving [accountId], valid until [expiresAt].
 * The client presents it only to refresh and logout — day-to-day requests carry the short-lived
 * [AccessToken] it mints. Revocation is a row delete; the client only ever holds the token.
 */
data class Session(val token: String, val accountId: AccountId, val expiresAt: Instant)
