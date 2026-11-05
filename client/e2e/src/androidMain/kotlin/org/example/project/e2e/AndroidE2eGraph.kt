package org.example.project.e2e

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.test.core.app.ActivityScenario
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.example.project.core.robots.E2eScope
import org.example.project.core.robots.Wait

/**
 * The instrumented host's driver graph: every contributed robot and journey, built around the
 * compose host of one test, plus the Activity levers only Android has. Ignorant of the app graph on
 * purpose: a flow that reaches below the UI hands that seam over at the call site.
 */
@DependencyGraph(E2eScope::class)
interface AndroidE2eGraph : E2eDrivers {
    val host: AndroidHost

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides nodes: SemanticsNodeInteractionsProvider,
            @Provides wait: Wait,
            @Provides scenario: ActivityScenario<out ComponentActivity>,
        ): AndroidE2eGraph
    }
}
