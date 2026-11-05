package org.example.project.feature.home.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import org.example.project.core.robots.E2eScope
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.home.HomeScreenTestTags

/** Drives the home screen through its semantics tags. */
@Inject
@ClassKey
@ContributesIntoMap(E2eScope::class)
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
