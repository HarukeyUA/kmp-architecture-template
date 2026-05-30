package org.example.project.server.database

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Runs [block] in a suspending database transaction against the application's default datasource.
 *
 * The **service layer owns the transaction**; repositories assume an ambient one (the
 * [JdbcTransaction] receiver) and never open their own — fixing the prior server experiment's
 * per-query `transaction { }` wrapping (ADR-0006).
 */
suspend fun <T> dbTransaction(block: suspend JdbcTransaction.() -> T): T =
    suspendTransaction(statement = block)
