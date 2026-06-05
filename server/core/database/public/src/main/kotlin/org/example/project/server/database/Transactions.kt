package org.example.project.server.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Runs [block] in a suspending database transaction against the application's default datasource.
 *
 * Exposed's [suspendTransaction] runs its blocking JDBC calls on **the caller's** dispatcher
 * (unlike the deprecated `newSuspendedTransaction`, it takes no context). The callers here are Ktor
 * request handlers, so this hop to [Dispatchers.IO] keeps the blocking driver work — including a
 * connection parked on [advisoryXactLock] — off the request-dispatch threads, matching
 * `AdvisoryLockScheduler`. That dispatcher boundary is the one thing this wrapper owns; it is the
 * single seam to widen later (isolation level, retry, metrics) without touching call sites.
 *
 * Orthogonally, the **service layer owns the transaction** (ADR-0006): repositories assume the
 * ambient [JdbcTransaction] receiver and never open their own. That discipline is enforced by the
 * module split, not by this function.
 */
suspend fun <T> dbTransaction(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) { suspendTransaction(statement = block) }

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
    exec(
        "SELECT pg_advisory_xact_lock(?, ?)",
        args = listOf(IntegerColumnType() to namespace, IntegerColumnType() to key),
    )
}
