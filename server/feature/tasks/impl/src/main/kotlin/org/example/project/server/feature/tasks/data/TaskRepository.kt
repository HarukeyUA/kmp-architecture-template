package org.example.project.server.feature.tasks.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.time.Clock
import org.example.project.server.auth.AccountId
import org.example.project.server.database.dbTransaction
import org.example.project.server.feature.tasks.Task
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning

/**
 * Every query is scoped to the owning account; that scoping is the authorization boundary. A
 * malformed, missing or foreign task id reads as absent: null, or false from [delete].
 */
interface TaskRepository {
    /** Newest first. */
    suspend fun listFor(accountId: AccountId): List<Task>

    suspend fun find(accountId: AccountId, taskId: String): Task?

    suspend fun create(accountId: AccountId, title: String): Task

    suspend fun setDone(accountId: AccountId, taskId: String, done: Boolean): Task?

    suspend fun delete(accountId: AccountId, taskId: String): Boolean
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultTaskRepository : TaskRepository {
    override suspend fun listFor(accountId: AccountId): List<Task> = dbTransaction {
        Tasks.selectAll()
            .where { Tasks.accountId eq accountId.value }
            .orderBy(Tasks.createdAt to SortOrder.DESC, Tasks.id to SortOrder.DESC)
            .map { it.toTask() }
    }

    override suspend fun find(accountId: AccountId, taskId: String): Task? {
        val id = taskId.toUuidOrNull() ?: return null
        return dbTransaction {
            Tasks.selectAll().where { owned(accountId, id) }.singleOrNull()?.toTask()
        }
    }

    // RETURNING because Postgres keeps microseconds: the row, not the clock, is what later reads
    // see.
    override suspend fun create(accountId: AccountId, title: String): Task = dbTransaction {
        Tasks.insertReturning {
                it[id] = UUID.randomUUID()
                it[Tasks.accountId] = accountId.value
                it[Tasks.title] = title
                it[done] = false
                it[createdAt] = Clock.System.now()
            }
            .single()
            .toTask()
    }

    override suspend fun setDone(accountId: AccountId, taskId: String, done: Boolean): Task? {
        val id = taskId.toUuidOrNull() ?: return null
        return dbTransaction {
            Tasks.updateReturning(where = { owned(accountId, id) }) { it[Tasks.done] = done }
                .singleOrNull()
                ?.toTask()
        }
    }

    override suspend fun delete(accountId: AccountId, taskId: String): Boolean {
        val id = taskId.toUuidOrNull() ?: return false
        return dbTransaction { Tasks.deleteWhere { owned(accountId, id) } > 0 }
    }

    private fun owned(accountId: AccountId, id: UUID): Op<Boolean> =
        (Tasks.id eq id) and (Tasks.accountId eq accountId.value)

    private fun String.toUuidOrNull(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun ResultRow.toTask(): Task =
        Task(
            id = this[Tasks.id].toString(),
            accountId = AccountId(this[Tasks.accountId]),
            title = this[Tasks.title],
            done = this[Tasks.done],
            createdAt = this[Tasks.createdAt],
        )
}
