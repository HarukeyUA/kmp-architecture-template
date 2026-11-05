package org.example.project.core.database

/** Runs a suspending block inside a single database transaction. */
interface TransactionRunner {
    /** Commits when [block] returns, and rolls back and rethrows when it throws. */
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
