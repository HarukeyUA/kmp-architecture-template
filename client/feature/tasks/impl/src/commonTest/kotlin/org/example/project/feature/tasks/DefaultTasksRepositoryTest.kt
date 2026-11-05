package org.example.project.feature.tasks

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.example.project.core.database.TransactionRunner
import org.example.project.core.database.entity.TaskEntity
import org.example.project.core.database.testing.FakeTaskDao
import org.example.project.core.database.testing.InlineTransactionRunner
import org.example.project.core.error.CallFailure
import org.example.project.feature.session.ClientSession
import org.example.project.feature.session.FakeSessionRepository
import org.example.project.feature.tasks.data.DefaultTasksRepository
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.encodeApiError
import org.example.project.shared.common.seamJson
import org.example.project.shared.tasks.TaskListResponse
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskResponse
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TaskValidationFailed
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksSetDoneError

class DefaultTasksRepositoryTest {
    private val sessions = FakeSessionRepository(ClientSession("access", "refresh"))
    private val dao = FakeTaskDao(listOf(CACHED))

    @Test
    fun `reads come from the cache without touching the server`() = runTest {
        val repository = repository { error("no read may reach the server") }

        assertThat(repository.observeTasks().first().map { it.id }).containsExactly(CACHED.id)
        assertThat(repository.observeTask(CACHED.id).first()?.title).isEqualTo(CACHED.title)
        assertThat(repository.observeTask("unknown").first()).isNull()
    }

    @Test
    fun `a task keeps its exact timestamp through the cache`() = runTest {
        val createdAt = Instant.parse("2026-05-01T10:15:30.123456Z")
        val repository = repository { respondJson(serverTask("a", createdAt = createdAt)) }

        repository.create(title("Anything")).getOrFail()

        assertThat(repository.observeTask("a").first()?.createdAt).isEqualTo(createdAt)
    }

    @Test
    fun `refresh replaces the cache with the server's list`() = runTest {
        val repository = repository { respondJson(TaskListResponse(listOf(serverTask("new")))) }

        repository.refresh().getOrFail()

        assertThat(dao.current.map { it.id }).containsExactly("new")
    }

    @Test
    fun `a refresh with no connection keeps the cache for offline reads`() = runTest {
        val repository = repository { throw IOException("offline") }

        val failure = repository.refresh().leftOrNull()

        assertThat(failure).isNotNull().isInstanceOf(CallFailure.Transport::class)
        assertThat(repository.observeTasks().first().map { it.id }).containsExactly(CACHED.id)
    }

    @Test
    fun `a refresh answered with a server error keeps the cache`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertThat(repository.refresh().leftOrNull())
            .isNotNull()
            .isInstanceOf(CallFailure.Transport::class)
        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `refreshing one task updates its row`() = runTest {
        val repository = repository { respondJson(serverTask(CACHED.id, done = true)) }

        repository.refresh(CACHED.id).getOrFail()

        assertThat(dao.current.single().done).isEqualTo(true)
    }

    @Test
    fun `refreshing a task the server no longer has drops it`() = runTest {
        val repository = repository { respondDeclared(serializer<TasksGetError>(), TaskNotFound) }

        val failure = repository.refresh(CACHED.id).leftOrNull()

        assertThat(failure).isEqualTo(CallFailure.Declared(TaskNotFound))
        assertThat(dao.current).isEmpty()
    }

    @Test
    fun `create writes through to the cache after the server took it`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repository { request ->
            requests += request
            respondJson(serverTask("new", title = "Buy milk"), HttpStatusCode.Created)
        }

        repository.create(title("Buy milk")).getOrFail()

        assertThat(requests.single().method).isEqualTo(HttpMethod.Post)
        assertThat(dao.current.map { it.title }).containsExactly("Buy milk", CACHED.title)
    }

    @Test
    fun `a create the server refused changes nothing and carries the Declared error`() = runTest {
        val rejection = TaskValidationFailed(listOf(FieldError("title", "blank")))
        val repository = repository { respondDeclared(serializer<TasksCreateError>(), rejection) }

        val failure = repository.create(title("Anything")).leftOrNull()

        assertThat(failure).isEqualTo(CallFailure.Declared<TasksCreateError>(rejection))
        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `a create with no connection fails and changes nothing`() = runTest {
        val repository = repository { throw IOException("offline") }

        val failure = repository.create(title("Anything")).leftOrNull()

        assertThat(failure).isNotNull().isInstanceOf(CallFailure.Transport::class)
        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `setDone writes the server's answer to the cache`() = runTest {
        val repository = repository { respondJson(serverTask(CACHED.id, done = true)) }

        repository.setDone(CACHED.id, done = true).getOrFail()

        assertThat(dao.current.single().done).isEqualTo(true)
    }

    @Test
    fun `setDone with no connection leaves the row as it was`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertThat(repository.setDone(CACHED.id, done = true).isLeft()).isEqualTo(true)
        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `setDone on a task the server no longer has reports it and drops the row`() = runTest {
        val repository = repository {
            respondDeclared(serializer<TasksSetDoneError>(), TaskNotFound)
        }

        val failure = repository.setDone(CACHED.id, done = true).leftOrNull()

        assertThat(failure).isEqualTo(CallFailure.Declared(TaskNotFound))
        assertThat(dao.current).isEmpty()
    }

    @Test
    fun `delete removes the row after the server did`() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        repository.delete(CACHED.id).getOrFail()

        assertThat(dao.current).isEmpty()
    }

    @Test
    fun `delete with no connection keeps the row`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertThat(repository.delete(CACHED.id).isLeft()).isEqualTo(true)
        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `deleting a task the server no longer has succeeds`() = runTest {
        val repository = repository {
            respondDeclared(serializer<TasksDeleteError>(), TaskNotFound)
        }

        repository.delete(CACHED.id).getOrFail()

        assertThat(dao.current).isEmpty()
    }

    @Test
    fun `an answer arriving after the session ended stays out of the wiped cache`() = runTest {
        val repository = repository {
            sessions.clear()
            dao.deleteAll()
            respondJson(TaskListResponse(listOf(serverTask("late"))))
        }

        repository.refresh().getOrFail()

        assertThat(dao.current).isEmpty()
    }

    @Test
    fun `an answer arriving after another sign-in stays out of its cache`() = runTest {
        val repository = repository {
            sessions.persist(ClientSession("access-b", "refresh-b"))
            respondJson(serverTask("late"))
        }

        repository.create(title("Anything")).getOrFail()

        assertThat(dao.current).containsExactly(CACHED)
    }

    @Test
    fun `a minted access token is the same sign-in and still reaches the cache`() = runTest {
        val repository = repository {
            sessions.persist(ClientSession("minted", "refresh"))
            respondJson(TaskListResponse(listOf(serverTask("new"))))
        }

        repository.refresh().getOrFail()

        assertThat(dao.current.map { it.id }).containsExactly("new")
    }

    @Test
    fun `a cache that cannot be written reports the failure instead of throwing`() = runTest {
        val failing =
            object : TransactionRunner {
                override suspend fun <T> runInTransaction(block: suspend () -> T): T =
                    error("disk gone")
            }
        val repository = repository(transactions = failing) { respondJson(serverTask("new")) }

        val failure = repository.create(title("Anything")).leftOrNull()

        assertThat(failure).isNotNull().isInstanceOf(CallFailure.Transport::class)
    }

    private fun repository(
        transactions: TransactionRunner = InlineTransactionRunner,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): TasksRepository =
        DefaultTasksRepository(
            client =
                HttpClient(MockEngine { request -> handler(request) }) {
                    install(ContentNegotiation) { json(seamJson) }
                    install(Resources)
                },
            dao = dao,
            transactions = transactions,
            sessions = sessions,
        )

    private inline fun <reified T> MockRequestHandleScope.respondJson(
        body: T,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData =
        respond(
            content = seamJson.encodeToString(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )

    private fun <E : ApiError> MockRequestHandleScope.respondDeclared(
        lens: KSerializer<E>,
        error: E,
    ): HttpResponseData = respondJson(ErrorEnvelope(encodeApiError(lens, error)), error.status)

    private fun title(raw: String): TaskTitle = checkNotNull(TaskTitle.of(raw).getOrNull())

    private fun <A : Any, B> Either<A, B>.getOrFail(): B =
        fold(ifLeft = { error("expected Right, was $it") }, ifRight = { it })

    private companion object {
        val CACHED = TaskEntity(id = "cached", title = "Cached", done = false, createdAt = 1)

        fun serverTask(
            id: String,
            title: String = id,
            done: Boolean = false,
            createdAt: Instant = Instant.fromEpochSeconds(1_780_000_000),
        ) = TaskResponse(id = id, title = title, done = done, createdAt = createdAt)
    }
}
