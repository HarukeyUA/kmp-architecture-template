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
 * Proof-of-identity material for the login path (ADR-0009): the account's id paired with its
 * Argon2id hash. Stays inside this `:impl` (the module boundary is the encapsulation — only
 * `:server:app` depends on `:impl`); the cross-module account model is the hash-free
 * [org.example.project.server.feature.auth.Account] in `:public`, so the hash's entire lifetime is
 * `login`'s verify step.
 */
data class Credential(val accountId: AccountId, val passwordHash: String)
