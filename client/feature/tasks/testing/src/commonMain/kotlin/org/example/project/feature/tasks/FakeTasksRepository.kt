package org.example.project.feature.tasks

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.example.project.core.error.CallFailure
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksSetDoneError

/** A scriptable repository. Each `next*Error` fails every matching call until it is cleared. */
class FakeTasksRepository(initial: List<Task> = emptyList()) : TasksRepository {
    private val tasks = MutableStateFlow(initial)

    var nextRefreshError: CallFailure<Nothing>? = null
    var nextRefreshTaskError: CallFailure<TasksGetError>? = null
    var nextCreateError: CallFailure<TasksCreateError>? = null
    var nextSetDoneError: CallFailure<TasksSetDoneError>? = null
    var nextDeleteError: CallFailure<TasksDeleteError>? = null

    /** Holds create, setDone and delete until completed, so a test can see them under way. */
    var writeGate: CompletableDeferred<Unit>? = null

    /** What the server answers a refresh with; null leaves the cache as it is. */
    var serverTasks: List<Task>? = null

    var refreshCalls: Int = 0
        private set

    val refreshedTaskIds: MutableList<String> = mutableListOf()
    val createdTitles: MutableList<String> = mutableListOf()

    val current: List<Task>
        get() = tasks.value

    override fun observeTasks(): Flow<List<Task>> = tasks

    override fun observeTask(id: String): Flow<Task?> = tasks.map { current ->
        current.firstOrNull { it.id == id }
    }

    override suspend fun refresh(): Either<CallFailure<Nothing>, Unit> {
        refreshCalls += 1
        nextRefreshError?.let {
            return it.left()
        }
        serverTasks?.let { tasks.value = it }
        return Unit.right()
    }

    override suspend fun refresh(id: String): Either<CallFailure<TasksGetError>, Unit> {
        refreshedTaskIds += id
        return nextRefreshTaskError?.left() ?: Unit.right()
    }

    override suspend fun create(title: TaskTitle): Either<CallFailure<TasksCreateError>, Unit> {
        writeGate?.await()
        nextCreateError?.let {
            return it.left()
        }
        createdTitles += title.value
        val task =
            Task(
                id = "created-${createdTitles.size}",
                title = title.value,
                done = false,
                createdAt = Instant.fromEpochSeconds(createdTitles.size.toLong()),
            )
        tasks.update { listOf(task) + it }
        return Unit.right()
    }

    override suspend fun setDone(
        id: String,
        done: Boolean,
    ): Either<CallFailure<TasksSetDoneError>, Unit> {
        writeGate?.await()
        nextSetDoneError?.let {
            return it.left()
        }
        tasks.update { current ->
            current.map { task -> if (task.id == id) task.copy(done = done) else task }
        }
        return Unit.right()
    }

    override suspend fun delete(id: String): Either<CallFailure<TasksDeleteError>, Unit> {
        writeGate?.await()
        nextDeleteError?.let {
            return it.left()
        }
        tasks.update { current -> current.filterNot { it.id == id } }
        return Unit.right()
    }
}
