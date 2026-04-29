package org.example.project.core.testing

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** How coroutines launched on [Dispatchers.Main] are driven during a [runLifecycleTest]. */
enum class LifecycleTestMainMode {
    /**
     * Main dispatcher is an [UnconfinedTestDispatcher] sharing the test scheduler. Launched
     * coroutines start eagerly on the calling thread; good for tests that assert on side effects
     * immediately after triggering them (e.g. checking a recorder right after calling `onEvent`).
     */
    Eager,

    /**
     * Main dispatcher is a [StandardTestDispatcher] sharing the test scheduler. Work runs only as
     * the scheduler advances; good for tests that need to observe each intermediate state emission
     * instead of having adjacent writes coalesced into one snapshot.
     */
    Queued,
}

fun runLifecycleTest(
    lifecycle: LifecycleRegistry = LifecycleRegistry(),
    mainMode: LifecycleTestMainMode = LifecycleTestMainMode.Queued,
    testBody: suspend TestScope.(lifecycle: LifecycleRegistry) -> Unit,
) =
    runTest(timeout = 10.seconds) {
        Dispatchers.setMain(mainMode.toDispatcher(testScheduler))
        lifecycle.resume()
        try {
            testBody(lifecycle)
        } finally {
            lifecycle.destroy()
            Dispatchers.resetMain()
        }
    }

fun runCoroutineTest(
    mainMode: LifecycleTestMainMode = LifecycleTestMainMode.Queued,
    testBody: suspend TestScope.() -> Unit,
) =
    runTest(timeout = 10.seconds) {
        Dispatchers.setMain(mainMode.toDispatcher(testScheduler))
        try {
            testBody()
        } finally {
            Dispatchers.resetMain()
        }
    }

private fun LifecycleTestMainMode.toDispatcher(scheduler: TestCoroutineScheduler): TestDispatcher =
    when (this) {
        LifecycleTestMainMode.Eager -> UnconfinedTestDispatcher(scheduler)
        LifecycleTestMainMode.Queued -> StandardTestDispatcher(scheduler)
    }
