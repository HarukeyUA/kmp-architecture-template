package org.example.project.integration

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlin.test.Test
import org.example.project.core.error.CallFailure
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.testing.serverTest
import org.example.project.shared.notes.NoteText
import org.example.project.shared.notes.NotesQuotaExceeded

/**
 * The client data layer against the real server — repository in, Postgres out. What the feature's
 * own MockEngine tests can't prove, this does: the *pair* of seam implementations agree. A create
 * through [org.example.project.feature.notes.data.NotesRepositoryImpl] is persisted by the real
 * routes/services, the session bearer attaches the token minted by the real signup, the
 * cross-domain author email round-trips, and the server's Declared error arrives as the client's
 * typed [CallFailure.Declared] through the same sealed lens.
 */
class NotesRepositoryIntegrationTest {

    @Test
    fun `signup, create, list, delete — the repository round trip`() = serverTest {
        val client = clientStack()

        // Signup through the real repository: tokens land in the session store, and from here the
        // bearer plugin authenticates every notes call.
        val signup = client.auth.signup("carol@example.com", "hunter2hunter2")
        assertThat(signup.isRight()).isEqualTo(true)
        assertThat(client.sessionStore.current()).isNotNull()

        val created = client.notes.create("Brew the moonwater")
        assertThat(created.isRight()).isEqualTo(true)

        // The listed note carries the author email the server resolved cross-domain (notes →
        // auth service) from the account this stack just created.
        val listed = client.notes.list().getOrNull().orEmpty()
        assertThat(listed).hasSize(1)
        assertThat(listed.single().text).isEqualTo("Brew the moonwater")
        assertThat(listed.single().author).isEqualTo("carol@example.com")

        assertThat(client.notes.delete(listed.single().id).isRight()).isEqualTo(true)
        assertThat(client.notes.list().getOrNull().orEmpty()).isEmpty()
    }

    @Test
    fun `the server's quota rejection arrives as the typed Declared error`() = serverTest {
        val client = clientStack()
        client.auth.signup("dana@example.com", "hunter2hunter2")

        // Fill the account's code-point budget through the real repository...
        val notesToFill = NotesService.QUOTA / NoteText.MAX_LENGTH
        repeat(notesToFill) {
            assertThat(client.notes.create("x".repeat(NoteText.MAX_LENGTH)).isRight())
                .isEqualTo(true)
        }

        // ... then the overflowing create narrows through the sealed lens into the exhaustive
        // Declared set — the same branch the notes UI switches on.
        val capped = client.notes.create("one over budget")
        val failure = capped.leftOrNull()
        assertThat(failure).isNotNull().isInstanceOf(CallFailure.Declared::class)
        val declared = (failure as CallFailure.Declared).error
        assertThat(declared)
            .isEqualTo(
                NotesQuotaExceeded(
                    quota = NotesService.QUOTA,
                    used = notesToFill * NoteText.MAX_LENGTH,
                )
            )
    }
}
