package org.example.project.server.feature.notes.data

import kotlin.time.Instant
import org.example.project.server.auth.AccountId
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * One row per note. `account_id` is intentionally **not** a FK to `accounts`: that table lives in
 * another domain's `:impl` which this module must not reach (ADR-0006). It is a plain indexed UUID
 * — the same decoupling the sessions table uses for its `account_id`.
 */
internal object Notes : Table("notes") {
    val id = javaUUID("id")
    val accountId = javaUUID("account_id")
    val text = text("text")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)

    init {
        index("notes_account_idx", false, accountId)
    }
}

/**
 * The notes domain model — never the Exposed `ResultRow` (ADR-0006). Visible across the `:impl`
 * module (the repository returns it, the service consumes it); encapsulation comes from the module
 * boundary — only `:server:app` depends on `:impl`.
 */
data class Note(val id: String, val accountId: AccountId, val text: String, val createdAt: Instant)
