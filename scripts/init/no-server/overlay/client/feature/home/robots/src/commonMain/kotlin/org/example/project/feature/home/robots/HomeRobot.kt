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
    fun awaitShown() {
        awaitTag("home screen", HomeScreenTestTags.TITLE)
    }
}
