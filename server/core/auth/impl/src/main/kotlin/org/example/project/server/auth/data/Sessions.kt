package org.example.project.server.auth.data

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * The opaque session store table. `account_id` is intentionally **not** a FK to `accounts`: that
 * table lives in the swappable Credential module (`:server:feature:auth`), which the invariant
 * session infra must not depend on (ADR-0009). Sessions are short-lived and revocable, so an orphan
 * on account deletion is a non-issue at this scope.
 *
 * Only the token digest is stored. The raw bearer token is shown to the client once and cannot be
 * recovered from the database.
 */
internal object Sessions : Table("sessions") {
    val tokenHash = text("token_hash")
    val accountId = javaUUID("account_id")
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    override val primaryKey = PrimaryKey(tokenHash)

    init {
        index("sessions_account_idx", false, accountId)
        index("sessions_expires_at_idx", false, expiresAt)
    }
}
