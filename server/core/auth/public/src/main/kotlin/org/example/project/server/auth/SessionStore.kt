package org.example.project.server.auth

/**
 * The **invariant** session infrastructure (ADR-0009): issue / resolve / revoke opaque sessions.
 * The implementation persists sessions in Postgres behind a short-TTL cache, so revocation
 * propagates within the TTL (bounded staleness, ADR-0010). The Credential module
 * (`:server:feature:auth`) is the swappable front-end that verifies a credential and calls [issue].
 */
interface SessionStore {
    /** Issues a fresh opaque session for [accountId]. */
    suspend fun issue(accountId: AccountId): Session

    /** Resolves a bearer [token] to its [Principal], or null if missing, expired, or revoked. */
    suspend fun resolve(token: String): Principal?

    /** Revokes the session for [token] (row delete + cache invalidation). */
    suspend fun revoke(token: String)
}
