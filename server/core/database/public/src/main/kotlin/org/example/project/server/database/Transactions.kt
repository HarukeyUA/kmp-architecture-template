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

/**
 * Takes a Postgres **transaction-scoped** advisory lock on the `(namespace, key)` pair: any other
 * transaction that locks the same pair blocks until this one commits or rolls back, at which point
 * the lock releases automatically (so it can never leak onto a pooled connection).
 *
 * Use it to make a read-then-write invariant safe under READ COMMITTED, where the read takes no row
 * locks and two concurrent transactions would otherwise each see a pre-write snapshot. Pick a
 * per-feature [namespace] so unrelated locks can't collide; [key] scopes the lock within it (e.g. a
 * per-account hash). Both are `Int`, matching `pg_advisory_xact_lock(int4, int4)`.
 */
fun JdbcTransaction.advisoryXactLock(namespace: Int, key: Int) {
    exec("SELECT pg_advisory_xact_lock($namespace, $key)")
}
