package org.example.project.feature.notes.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.notes.NotesScreenTestTags

/** Drives the notes screen through its semantics tags. */
class NotesRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    fun awaitShown() {
        awaitTag("notes screen", NotesScreenTestTags.INPUT)
    }

    fun typeNote(text: String) {
        nodes.onNodeWithTag(NotesScreenTestTags.INPUT).performTextInput(text)
    }

    /** Taps "Add note" once it enables (it is state-gated on a non-blank input). */
    fun addNote() {
        awaitEnabled("add-note button enabled", NotesScreenTestTags.ADD)
        clickTag(NotesScreenTestTags.ADD)
    }

    /** Waits for a note card whose content contains [text] — a content assertion, hence text. */
    fun awaitNoteShown(text: String) {
        await("note containing \"$text\"") {
            nodes.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
