package org.example.project.server.feature.auth.data

import org.example.project.server.auth.AccountId
import org.example.project.server.database.utcTimestamp
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

/** The credential store: one row per account, holding the Argon2id password hash. */
internal object Accounts : Table("accounts") {
    val id = javaUUID("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val createdAt = utcTimestamp("created_at")
    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("accounts_email_idx", email)
    }
}

/**
 * The server's account domain model — never the Exposed `ResultRow` (ADR-0006). Visible across the
 * `:impl` module (the repository returns it, the service consumes it); encapsulation comes from the
 * module boundary — only `:server:app` depends on `:impl`.
 */
data class Account(val id: AccountId, val email: String, val passwordHash: String)
