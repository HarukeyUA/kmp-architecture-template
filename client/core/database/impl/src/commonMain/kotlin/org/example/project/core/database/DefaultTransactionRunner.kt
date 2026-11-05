package org.example.project.core.database

import androidx.room3.withWriteTransaction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class DefaultTransactionRunner(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T =
        database.withWriteTransaction {
            block()
        }
}
