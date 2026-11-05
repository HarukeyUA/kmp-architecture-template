package org.example.project.feature.tasks

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import org.example.project.core.error.CallFailure
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksSetDoneError

/**
 * The signed-in account's tasks. The server owns them and this device keeps a read cache: `observe`
 * reads the cache, so it works offline, while every write has to reach the server and only then
 * lands in the cache. A write that fails changes nothing.
 */
interface TasksRepository {
    /** The cached tasks, newest first, re-emitted whenever the cache changes. */
    fun observeTasks(): Flow<List<Task>>

    /** The cached task, or null while the cache holds no such task. */
    fun observeTask(id: String): Flow<Task?>

    /** Replaces the cache with the server's list. A failure leaves the cache as it was. */
    suspend fun refresh(): Either<CallFailure<Nothing>, Unit>

    /** Updates one cached task from the server, dropping it if the server no longer has it. */
    suspend fun refresh(id: String): Either<CallFailure<TasksGetError>, Unit>

    suspend fun create(title: TaskTitle): Either<CallFailure<TasksCreateError>, Unit>

    /** A task the server no longer has is also dropped from the cache, besides the failure. */
    suspend fun setDone(id: String, done: Boolean): Either<CallFailure<TasksSetDoneError>, Unit>

    /** Also succeeds over a task the server no longer has, since the task is gone either way. */
    suspend fun delete(id: String): Either<CallFailure<TasksDeleteError>, Unit>
}
