package org.example.project.feature.tasks.data

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.TransactionRunner
import org.example.project.core.database.dao.TaskDao
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.network.call
import org.example.project.feature.session.SessionStatus
import org.example.project.feature.session.boundSessionOrNull
import org.example.project.feature.tasks.Task
import org.example.project.feature.tasks.TasksRepository
import org.example.project.shared.common.ApiError
import org.example.project.shared.tasks.CreateTaskRequest
import org.example.project.shared.tasks.SetTaskDoneRequest
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TasksApi
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksResource
import org.example.project.shared.tasks.TasksSetDoneError

@Inject
@ContributesBinding(AppScope::class)
class DefaultTasksRepository(
    private val client: HttpClient,
    private val dao: TaskDao,
    private val transactions: TransactionRunner,
    private val sessions: SessionStatus,
) : TasksRepository {
    private val log = Logger.withTag("TasksRepository")

    override fun observeTasks(): Flow<List<Task>> =
        dao.observeAll().map { rows -> rows.map { it.toTask() } }

    override fun observeTask(id: String): Flow<Task?> = dao.observeById(id).map { it?.toTask() }

    override suspend fun refresh(): Either<CallFailure<Nothing>, Unit> = either {
        val signIn = currentSignIn()
        val response = client.call(TasksApi.list, TasksResource()).bind()
        cacheFor(signIn) {
                dao.deleteAll()
                dao.upsertAll(response.tasks.map { it.toEntity() })
            }
            .bind()
    }

    override suspend fun refresh(id: String): Either<CallFailure<TasksGetError>, Unit> = either {
        val signIn = currentSignIn()
        val response =
            client
                .call(TasksApi.get, TasksResource.ById(id = id))
                .dropFromCacheIfNotFound(signIn, id)
                .bind()
        cacheFor(signIn) { dao.upsert(response.toEntity()) }.bind()
    }

    override suspend fun create(title: TaskTitle): Either<CallFailure<TasksCreateError>, Unit> =
        either {
            val signIn = currentSignIn()
            val response =
                client.call(TasksApi.create, TasksResource(), CreateTaskRequest(title.value)).bind()
            cacheFor(signIn) { dao.upsert(response.toEntity()) }.bind()
        }

    override suspend fun setDone(
        id: String,
        done: Boolean,
    ): Either<CallFailure<TasksSetDoneError>, Unit> = either {
        val signIn = currentSignIn()
        val response =
            client
                .call(TasksApi.setDone, TasksResource.ById(id = id), SetTaskDoneRequest(done))
                .dropFromCacheIfNotFound(signIn, id)
                .bind()
        cacheFor(signIn) { dao.upsert(response.toEntity()) }.bind()
    }

    override suspend fun delete(id: String): Either<CallFailure<TasksDeleteError>, Unit> {
        val signIn = currentSignIn()
        return client
            .call(TasksApi.delete, TasksResource.ById(id = id))
            .fold(
                ifLeft = { failure ->
                    if (failure.isTaskNotFound()) cacheFor(signIn) { dao.deleteById(id) }
                    else Either.Left(failure)
                },
                ifRight = { cacheFor(signIn) { dao.deleteById(id) } },
            )
    }

    private suspend fun <E : ApiError, A> Either<CallFailure<E>, A>.dropFromCacheIfNotFound(
        signIn: String?,
        id: String,
    ): Either<CallFailure<E>, A> = onLeft { failure ->
        if (failure.isTaskNotFound()) cacheFor(signIn) { dao.deleteById(id) }
    }

    private fun CallFailure<*>.isTaskNotFound(): Boolean =
        this is CallFailure.Declared && error == TaskNotFound

    private suspend fun currentSignIn(): String? = sessions.boundSessionOrNull(log)?.refreshToken

    // Applies write only while signIn, the session that made the call, is still current: an answer
    // arriving after a logout must not refill the cache the logout wiped. The check runs in the
    // write transaction because a wipe needs that same writer: either this write lands first and
    // the wipe removes it, or the wipe lands first and the session has already changed.
    //
    // The server already holds the outcome by now, so a local failure is reported the way any other
    // unusable answer is.
    private suspend fun cacheFor(
        signIn: String?,
        write: suspend () -> Unit,
    ): Either<CallFailure<Nothing>, Unit> =
        Either.catch {
                transactions.runInTransaction {
                    if (signIn != null && signIn == currentSignIn()) write()
                }
            }
            .fold(
                ifLeft = { Either.Left(CallFailure.Transport(NetworkError.Serialization(it))) },
                ifRight = { it.right() },
            )
}
