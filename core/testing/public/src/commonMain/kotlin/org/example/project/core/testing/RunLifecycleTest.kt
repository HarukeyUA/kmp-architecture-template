package org.example.project.core.testing

import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

fun runLifecycleTest(
    lifecycle: LifecycleRegistry = LifecycleRegistry(),
    testBody: suspend TestScope.(lifecycle: LifecycleRegistry) -> Unit,
) =
    runTest(timeout = 10.seconds) {
        lifecycle.resume()
        testBody(lifecycle)
        lifecycle.destroy()
    }
