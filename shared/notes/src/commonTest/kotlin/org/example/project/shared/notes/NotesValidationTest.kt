package org.example.project.shared.notes

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/** Shape-validation rules for notes (the same code the client and server both run, ADR-0004). */
class NotesValidationTest {
    @Test
    fun `blank text is rejected`() {
        assertThat(NoteText.of("   ").leftOrNull()?.code).isEqualTo("blank")
    }

    @Test
    fun `text within the limit is accepted verbatim`() {
        assertThat(NoteText.of("Milk, eggs").getOrNull()?.value).isEqualTo("Milk, eggs")
    }

    @Test
    fun `over-long text is rejected`() {
        val tooLong = "a".repeat(NoteText.MAX_LENGTH + 1)
        assertThat(NoteText.of(tooLong).leftOrNull()?.code).isEqualTo("too_long")
    }
}
