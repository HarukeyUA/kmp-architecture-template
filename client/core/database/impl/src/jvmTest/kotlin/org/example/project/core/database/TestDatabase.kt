package org.example.project.core.database

import androidx.room3.Room
import androidx.room3.immediateTransaction
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.example.project.core.database.di.DatabaseGraph

/** The table these helpers write to. */
internal const val TEST_TABLE = "tasks"

/** A file database, not an in-memory one, so reads and writes use separate connections. */
internal fun openTestDatabase(directory: Path): AppDatabase =
    DatabaseGraph.provideAppDatabase(
        Room.databaseBuilder(
            name = directory.resolve("test.db").toString(),
            factory = AppDatabaseConstructor::initialize,
        ),
        Dispatchers.IO,
    )

internal suspend fun AppDatabase.insertRow(id: Long) {
    useWriterConnection { connection ->
        connection.immediateTransaction {
            usePrepared(
                "INSERT INTO $TEST_TABLE (id, title, done, created_at) VALUES (?, '', 0, 0)",
            ) { statement ->
                statement.bindText(1, id.toString())
                statement.step()
            }
        }
    }
}

internal suspend fun AppDatabase.rowCount(): Long = useReaderConnection { connection ->
    connection.usePrepared("SELECT COUNT(*) FROM $TEST_TABLE") { statement ->
        statement.step()
        statement.getLong(0)
    }
}
