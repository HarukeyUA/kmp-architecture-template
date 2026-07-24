package org.example.project.server

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.testing.decodedError
import org.example.project.server.testing.serverTest
import org.example.project.server.testing.signupViaApi
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NoteText
import org.example.project.shared.notes.NotesQuotaExceeded
import org.example.project.shared.notes.NotesResource

/**
 * The Phase 5 validation gate (ADR-0006): the notes domain driven through its real routes against
 * the shared [org.example.project.server.testing.TestPostgres], proving cheap replication of the
 * auth slice's shape plus the two things Phase 5 is actually about — (1) the **cross-domain** call:
 * each note carries the `authorEmail` that the notes service resolved through `AuthService` (a
 * feature→feature `:public` call, never the accounts table); (2) the per-domain
 * `NotesQuotaExceeded` error round-trips through the shared seam. `:server:app` itself gained zero
 * production lines — the routes/table/error module all self-register.
 */
class NotesIntegrationTest {
    @Test
    fun `notes CRUD, cross-domain author, and the per-account quota`() = serverTest { client ->
        // A note is owned by whoever the tokens belong to, so sign up first.
        val token = signupViaApi(client, email = "alice@example.com").accessToken

        // Unauthenticated access is rejected by the JWT middleware.
        val unauthenticated = client.get(NotesResource())
        assertThat(unauthenticated.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(unauthenticated.decodedError()).isEqualTo(Unauthorized)

        // Create → 201, and the author email came from the cross-domain AuthService call.
        val created = client.createNote(token, "Milk, eggs")
        assertThat(created.status).isEqualTo(HttpStatusCode.Created)
        val note = created.body<NoteResponse>()
        assertThat(note.authorEmail).isEqualTo("alice@example.com")

        // List → the one note, with the resolved author.
        val listed = client.get(NotesResource()) { bearerAuth(token) }.body<NoteListResponse>()
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

        // Fill the account's code-point budget with astral-plane notes (each 🦀 is one
        // code point but two UTF-16 units), then the next create surfaces the typed error.
        // This locks the quota unit to code points end-to-end: counted by `.length`, the
        // second fill note would already burst the budget and `used` would misreport.
        val notesToFill = NotesService.QUOTA / NoteText.MAX_LENGTH
        repeat(notesToFill) { client.createNote(token, "🦀".repeat(NoteText.MAX_LENGTH)) }
        val capped = client.createNote(token, "one over budget")
        assertThat(capped.status).isEqualTo(HttpStatusCode.Conflict)
        assertThat(capped.decodedError())
            .isEqualTo(NotesQuotaExceeded(NotesService.QUOTA, notesToFill * NoteText.MAX_LENGTH))
    }

    /** Posts a note as [token]'s owner — the shared shape of the three creates above. */
    private suspend fun HttpClient.createNote(token: String, text: String) =
        post(NotesResource()) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateNoteRequest(text))
        }
}
