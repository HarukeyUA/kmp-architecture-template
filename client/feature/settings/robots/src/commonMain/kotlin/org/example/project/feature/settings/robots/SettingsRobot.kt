package org.example.project.feature.settings.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.settings.SettingsScreenTestTags

/** Drives the settings screen through its semantics tags. */
class SettingsRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    fun awaitShown() {
        awaitTag("settings screen", SettingsScreenTestTags.LOG_OUT)
    }

    fun selectTheme(mode: ThemeMode) {
        clickTag(SettingsScreenTestTags.theme(mode))
    }

    fun awaitThemeSelected(mode: ThemeMode) {
        await("$mode theme selected") {
            val matcher = hasTestTag(SettingsScreenTestTags.theme(mode)) and isSelected()
            nodes.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Navigation leaves this feature, so waiting for the login screen belongs to the flow. */
    fun logOut() {
        clickTag(SettingsScreenTestTags.LOG_OUT)
    }
}
