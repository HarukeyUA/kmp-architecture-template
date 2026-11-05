package org.example.project.feature.home.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.home.HomeScreenTestTags

/** Drives the home screen through its semantics tags. */
class HomeRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    /** Waits for the home screen, the signed-in landing state. */
    fun awaitShown() {
        awaitTag("home screen", HomeScreenTestTags.LOG_OUT)
    }

    /** Navigation leaves this feature, so waiting for the login screen belongs to the flow. */
    fun logOut() {
        clickTag(HomeScreenTestTags.LOG_OUT)
    }
}
