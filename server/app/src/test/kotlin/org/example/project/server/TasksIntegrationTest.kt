package org.example.project.server

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import org.example.project.server.testing.decodedError
import org.example.project.server.testing.serverTest
import org.example.project.server.testing.signupViaApi
import org.example.project.shared.common.BadRequest
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.tasks.CreateTaskRequest
import org.example.project.shared.tasks.SetTaskDoneRequest
import org.example.project.shared.tasks.TaskListResponse
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskResponse
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TaskValidationFailed
import org.example.project.shared.tasks.TasksResource

class TasksIntegrationTest {
    @Test
    fun `every task route requires an access token`() = serverTest { client ->
        val byId = TasksResource.ById(id = UUID.randomUUID().toString())
        val responses =
            listOf(
                client.get(TasksResource()),
                client.post(TasksResource()) { json(CreateTaskRequest("Buy milk")) },
                client.get(byId),
                client.patch(byId) { json(SetTaskDoneRequest(done = true)) },
                client.delete(byId),
            )

        responses.forEach {
            assertThat(it.status).isEqualTo(HttpStatusCode.Unauthorized)
            assertThat(it.decodedError()).isEqualTo(Unauthorized)
        }
    }

    @Test
    fun `list, get, create, set done and delete`() = serverTest { client ->
        val token = signupViaApi(client).accessToken

        val created = client.createTask(token, "  Buy milk ")
        assertThat(created.status).isEqualTo(HttpStatusCode.Created)
        val first = created.body<TaskResponse>()
        assertThat(first.title).isEqualTo("Buy milk")
        assertThat(first.done).isEqualTo(false)
        val second = client.createTask(token, "Walk the dog").body<TaskResponse>()

        assertThat(client.listTasks(token)).containsExactly(second, first)

        val byId = TasksResource.ById(id = first.id)
        assertThat(client.get(byId) { bearerAuth(token) }.body<TaskResponse>()).isEqualTo(first)

        val done = client.setDone(token, byId, done = true)
        assertThat(done.status).isEqualTo(HttpStatusCode.OK)
        assertThat(done.body<TaskResponse>()).isEqualTo(first.copy(done = true))
        assertThat(client.get(byId) { bearerAuth(token) }.body<TaskResponse>().done).isEqualTo(true)
        assertThat(client.setDone(token, byId, done = false).body<TaskResponse>()).isEqualTo(first)

        assertThat(client.delete(byId) { bearerAuth(token) }.status)
            .isEqualTo(HttpStatusCode.NoContent)
        client.delete(byId) { bearerAuth(token) }.assertTaskNotFound()
        client.get(byId) { bearerAuth(token) }.assertTaskNotFound()
        assertThat(client.listTasks(token)).containsExactly(second)
    }

    @Test
    fun `another account's task behaves as not found`() = serverTest { client ->
        val alice = signupViaApi(client, email = "alice@example.com").accessToken
        val bob = signupViaApi(client, email = "bob@example.com").accessToken
        val task = client.createTask(alice, "Alice's").body<TaskResponse>()
        val byId = TasksResource.ById(id = task.id)

        assertThat(client.listTasks(bob)).isEmpty()
        client.get(byId) { bearerAuth(bob) }.assertTaskNotFound()
        client.setDone(bob, byId, done = true).assertTaskNotFound()
        client.delete(byId) { bearerAuth(bob) }.assertTaskNotFound()

        assertThat(client.listTasks(alice)).containsExactly(task)
    }

    @Test
    fun `a missing or malformed id is not found`() = serverTest { client ->
        val token = signupViaApi(client).accessToken

        listOf(UUID.randomUUID().toString(), "not-a-uuid").forEach { id ->
            val byId = TasksResource.ById(id = id)
            client.get(byId) { bearerAuth(token) }.assertTaskNotFound()
            client.setDone(token, byId, done = true).assertTaskNotFound()
            client.delete(byId) { bearerAuth(token) }.assertTaskNotFound()
        }
    }

    @Test
    fun `an invalid title is rejected and nothing is stored`() = serverTest { client ->
        val token = signupViaApi(client).accessToken

        val invalid =
            mapOf(
                "   " to "blank",
                "a".repeat(TaskTitle.MAX_LENGTH + 1) to "too_long",
                "a\u0000b" to "format",
            )

        invalid.forEach { (title, code) ->
            val response = client.createTask(token, title)
            assertThat(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
            assertThat(response.decodedError())
                .isEqualTo(TaskValidationFailed(listOf(FieldError("title", code))))
        }
        assertThat(client.listTasks(token)).isEmpty()
    }

    @Test
    fun `a malformed body is a bad request`() = serverTest { client ->
        val token = signupViaApi(client).accessToken

        val response =
            client.post(TasksResource()) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("{\"title\": 1")
            }

        assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(response.decodedError()).isEqualTo(BadRequest("malformed_body"))
    }

    private suspend fun HttpResponse.assertTaskNotFound() {
        assertThat(status).isEqualTo(HttpStatusCode.NotFound)
        assertThat(decodedError()).isEqualTo(TaskNotFound)
    }

    private suspend fun HttpClient.listTasks(token: String): List<TaskResponse> =
        get(TasksResource()) { bearerAuth(token) }.body<TaskListResponse>().tasks

    private suspend fun HttpClient.createTask(token: String, title: String): HttpResponse =
        post(TasksResource()) {
            bearerAuth(token)
            json(CreateTaskRequest(title))
        }

    private suspend fun HttpClient.setDone(
        token: String,
        byId: TasksResource.ById,
        done: Boolean,
    ): HttpResponse =
        patch(byId) {
            bearerAuth(token)
            json(SetTaskDoneRequest(done))
        }
}

private inline fun <reified T : Any> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
