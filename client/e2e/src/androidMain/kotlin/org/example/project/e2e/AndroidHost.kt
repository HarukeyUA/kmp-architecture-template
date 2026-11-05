package org.example.project.e2e

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import dev.zacsweers.metro.Inject

/**
 * What an instrumented flow does to the Activity or the task rather than to a screen. Nothing here
 * has a desktop counterpart, which is why no journey abstracts over it.
 */
@Inject
class AndroidHost(private val scenario: ActivityScenario<out ComponentActivity>) {

    /** System back, the way a user leaves a step that has no back button of its own. */
    fun pressBack() {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    /** Recreates the Activity while preserving the test account's local app data. */
    fun recreateActivity() {
        scenario.recreate()
    }
}
