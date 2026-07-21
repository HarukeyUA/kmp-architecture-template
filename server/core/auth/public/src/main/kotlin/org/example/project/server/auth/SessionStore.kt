package org.example.project.server.auth

/**
 * The **invariant** session infrastructure (ADR-0009): issue / resolve / revoke opaque sessions.
 * Since the JWT amendment a Session is the *refresh* credential — [resolve] runs on the refresh
 * endpoint rather than on every request. The implementation persists sessions in Postgres behind a
 * short-TTL cache, so revocation propagates within the TTL (bounded staleness, ADR-0010). The
 * Credential module (`:server:feature:auth`) is the swappable front-end that verifies a credential
 * and calls [issue].
 */
interface SessionStore {
    /** Issues a fresh opaque session for [accountId]. */
    suspend fun issue(accountId: AccountId): Session

    /** Resolves a session [token] to its [Principal], or null if missing, expired, or revoked. */
    suspend fun resolve(token: String): Principal?

    /** Revokes the session for [token] (row delete + cache invalidation). */
    suspend fun revoke(token: String)

    /**
     * Revokes **every** session for [accountId] — the password-change / compromise / account-
     * deletion primitive. Same staleness contract as [revoke]: immediate on the current node (every
     * existing token hash is tombstoned), bounded by the cache TTL on other nodes (ADR-0010).
     * Already-minted access tokens stay valid until they expire ([JwtConfig.accessTokenTtl]) —
     * revocation here cuts off *refresh*, not in-flight JWTs.
     */
    suspend fun revokeAllFor(accountId: AccountId)
}
