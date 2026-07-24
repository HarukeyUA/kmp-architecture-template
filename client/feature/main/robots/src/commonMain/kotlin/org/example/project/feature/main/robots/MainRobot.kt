package org.example.project.feature.main.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.main.presentation.MainScreenTestTags

/** Drives the main tab scaffold through its semantics tags. */
class MainRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    /** Waits for the tab bar — the signed-in landing state. */
    fun awaitShown() {
        awaitTag("main screen", MainScreenTestTags.HOME_TAB)
    }

    /** Opens the Notes tab; the notes screen itself belongs to another feature's robot. */
    fun openNotesTab() {
        clickTag(MainScreenTestTags.NOTES_TAB)
    }
}
