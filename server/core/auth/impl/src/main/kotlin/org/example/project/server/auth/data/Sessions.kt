package org.example.project.server.auth.data

import org.example.project.server.database.instantTimestampTz
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

/**
 * The opaque session store table. `account_id` is intentionally **not** a FK to `accounts`: that
 * table lives in the swappable Credential module (`:server:feature:auth`), which the invariant
 * session infra must not depend on (ADR-0009). Sessions are short-lived and revocable, so an orphan
 * on account deletion is a non-issue at this scope.
 */
internal object Sessions : Table("sessions") {
    val token = text("token")
    val accountId = javaUUID("account_id")
    val createdAt = instantTimestampTz("created_at")
    val expiresAt = instantTimestampTz("expires_at")
    override val primaryKey = PrimaryKey(token)

    init {
        index("sessions_account_idx", false, accountId)
        index("sessions_expires_at_idx", false, expiresAt)
    }
}
