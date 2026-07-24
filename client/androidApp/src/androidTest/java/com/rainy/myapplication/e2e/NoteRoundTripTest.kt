package com.rainy.myapplication.e2e

import kotlin.random.Random
import org.junit.Test

/**
 * The template's example E2E flow: signs up a fresh account through the real UI and creates the
 * account's first note against the real dev server. The note card only renders after the create
 * round-trips and the list is re-fetched, and its author line is the account email resolved
 * server-side (a cross-domain call into the auth service) — so both assertions prove live
 * client↔server traffic, not local state.
 */
class NoteRoundTripTest : E2eTest() {

    @Test
    fun signupAndFirstNoteRoundTripTheServer() {
        // Unique per test: the Orchestrator clears app data between tests, but the dev server's
        // database persists across runs (isolation by account-per-test).
        val email = "e2e-${Random.nextLong(Long.MAX_VALUE)}@example.com"
        signUpAndAwaitMain(email, PASSWORD)

        main.openNotesTab()
        notes.awaitShown()

        notes.typeNote(NOTE_TEXT)
        notes.addNote()

        notes.awaitNoteShown(NOTE_TEXT)
        notes.awaitNoteShown(email)
    }

    private companion object {
        const val PASSWORD = "correct-horse-battery-staple"
        const val NOTE_TEXT = "First note, straight through the seam"
    }
}
