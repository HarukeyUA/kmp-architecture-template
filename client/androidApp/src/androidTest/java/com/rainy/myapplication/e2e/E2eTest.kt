package com.rainy.myapplication.e2e

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rainy.myapplication.MainActivity
import dev.zacsweers.metro.createGraphFactory
import kotlin.random.Random
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.core.robots.await
import org.example.project.e2e.AndroidE2eGraph
import org.example.project.e2e.AndroidHost
import org.example.project.e2e.Journey
import org.example.project.e2e.journey
import org.example.project.e2e.robot
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.RuleChain

/**
 * Base for the E2E flows, run by `scripts/e2e-android.sh` against the dev server. A flow declares
 * its drivers as lazy delegates (`by robot<TasksRobot>()`, `by journey<AccountJourney>()`) and
 * reads what it needs from below the UI off [e2eGraph]; nothing is wired here.
 */
abstract class E2eTest {

    protected val compose = createAndroidComposeRule<MainActivity>()

    // Compose rule outside the watcher: the compose host must still be alive when the failure
    // screenshot is captured.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(FailureScreenshotRule(compose))

    private val wait = Wait { description, timeoutMillis, condition ->
        compose.waitUntil(description, timeoutMillis, condition)
    }

    /** Built on first use, once the rule has launched the Activity the host levers need. */
    protected val drivers: AndroidE2eGraph by lazy {
        createGraphFactory<AndroidE2eGraph.Factory>()
            .create(nodes = compose, wait = wait, scenario = compose.activityRule.scenario)
    }

    protected inline fun <reified T : Robot> robot(): Lazy<T> = lazy { drivers.robot<T>() }

    protected inline fun <reified T : Journey> journey(): Lazy<T> = lazy { drivers.journey<T>() }

    /** The Activity and task levers: system back, recreation. */
    protected val host: AndroidHost
        get() = drivers.host

    /** The app under test, which [E2eTestRunner] installed in place of the shipped Application. */
    protected val app: TestMyApplication
        get() =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as TestMyApplication

    /** The running app's own graph, widened to the accessors described on [E2eAppGraph]. */
    protected val e2eGraph: E2eAppGraph
        get() = app.appGraph as E2eAppGraph

    /** Waits up to the standard robot timeout for a flow-level [condition]. */
    protected fun await(description: String, condition: () -> Boolean) {
        wait.await(description, condition)
    }

    /** Every run needs a fresh account: the Orchestrator clears app data, not the dev database. */
    protected fun freshEmail(): String = "e2e-${Random.nextLong(Long.MAX_VALUE)}@example.com"

    companion object {
        const val PASSWORD = "correct-horse-battery-staple"

        @JvmField @ClassRule val devServer: DevServerHealthRule = DevServerHealthRule()
    }
}
