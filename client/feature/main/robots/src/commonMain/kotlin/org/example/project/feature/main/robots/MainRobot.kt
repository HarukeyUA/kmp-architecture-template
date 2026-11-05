package org.example.project.feature.main.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.main.MainScreenTestTags

/** Drives the tab navigation, which is a bottom bar or a rail depending on the window. */
class MainRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    /** Waits for the tabs, the signed-in landing state. */
    fun awaitShown() {
        awaitTag("main screen", MainScreenTestTags.TASKS_TAB)
    }

    fun openTasksTab() {
        clickTag(MainScreenTestTags.TASKS_TAB)
    }

    fun openSettingsTab() {
        clickTag(MainScreenTestTags.SETTINGS_TAB)
    }
}
