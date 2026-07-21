package org.example.project.server.feature.notes.data

import org.example.project.server.database.utcTimestamp
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

/**
 * One row per note. `account_id` is intentionally **not** a FK to `accounts`: that table lives in
 * another domain's `:impl` which this module must not reach (ADR-0006). It is a plain indexed UUID
 * — the same decoupling the sessions table uses for its `account_id`.
 */
internal object Notes : Table("notes") {
    val id = javaUUID("id")
    val accountId = javaUUID("account_id")
    val text = text("text")
    val createdAt = utcTimestamp("created_at")
    override val primaryKey = PrimaryKey(id)

    init {
        index("notes_account_idx", false, accountId)
    }
}
