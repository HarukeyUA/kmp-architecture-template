package org.example.project.server

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.feature.notes.NotesService
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.authErrorSerializersModule
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.buildSeamJson
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NoteText
import org.example.project.shared.notes.NotesQuotaExceeded
import org.example.project.shared.notes.NotesResource
import org.example.project.shared.notes.notesErrorSerializersModule
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The Phase 5 validation gate (ADR-0006): the notes domain driven through its real routes against a
 * Testcontainers Postgres, proving cheap replication of the auth slice's shape plus the two things
 * Phase 5 is actually about — (1) the **cross-domain** call: each note carries the `authorEmail`
 * that the notes service resolved through `AuthService` (a feature→feature `:public` call, never
 * the accounts table); (2) the per-domain `NotesQuotaExceeded` error round-trips through the shared
 * seam. `:server:app` itself gained zero production lines — the routes/table/error module all
 * self-register.
 */
class NotesIntegrationTest {
    @Test
    fun `notes CRUD, cross-domain author, and the per-account quota`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val databaseConfig =
                DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(
                        ServerConfig("localhost", port = 0, version = "test", databaseConfig),
                        databaseConfig,
                    )
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            buildSeamJson(
                                setOf(authErrorSerializersModule, notesErrorSerializersModule)
                            )
                        )
                    }
                    install(Resources)
                }

                // A note is owned by whoever the session belongs to, so sign up first.
                val token =
                    client
                        .post(AuthResource.Signup()) {
                            contentType(ContentType.Application.Json)
                            setBody(SignupRequest("alice@example.com", "hunter2hunter2"))
                        }
                        .body<SessionResponse>()
                        .token

                // Unauthenticated access is rejected by the session middleware.
                assertThat(client.get(NotesResource()).status)
                    .isEqualTo(HttpStatusCode.Unauthorized)

                // Create → 201, and the author email came from the cross-domain AuthService call.
                val created = client.createNote(token, "Milk, eggs")
                assertThat(created.status).isEqualTo(HttpStatusCode.Created)
                val note = created.body<NoteResponse>()
                assertThat(note.authorEmail).isEqualTo("alice@example.com")

                // List → the one note, with the resolved author.
                val listed =
                    client.get(NotesResource()) { bearerAuth(token) }.body<NoteListResponse>()
                assertThat(listed.notes).hasSize(1)
                assertThat(listed.notes.single().text).isEqualTo("Milk, eggs")
                assertThat(listed.notes.single().authorEmail).isEqualTo("alice@example.com")

                // Delete the owned note → 204; deleting it again → 404 (missing and not-yours
                // alike).
                val byId = NotesResource.ById(id = note.id)
                assertThat(client.delete(byId) { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.NoContent)
                assertThat(client.delete(byId) { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.NotFound)

                // Fill the account's character budget, then the next create surfaces the typed
                // error.
                val notesToFill = NotesService.QUOTA / NoteText.MAX_LENGTH
                repeat(notesToFill) { client.createNote(token, "a".repeat(NoteText.MAX_LENGTH)) }
                val capped = client.createNote(token, "one over budget")
                assertThat(capped.body<ErrorEnvelope>().error)
                    .isEqualTo(
                        NotesQuotaExceeded(NotesService.QUOTA, notesToFill * NoteText.MAX_LENGTH)
                    )
            }
        }
    }

    /** Posts a note as [token]'s owner — the shared shape of the three creates above. */
    private suspend fun HttpClient.createNote(token: String, text: String) =
        post(NotesResource()) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateNoteRequest(text))
        }
}
