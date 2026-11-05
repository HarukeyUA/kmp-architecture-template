package org.example.project.core.database.testing

import org.example.project.core.database.TransactionRunner

/** Runs the block with no rollback, for fakes that have no database to wrap. */
object InlineTransactionRunner : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
}
