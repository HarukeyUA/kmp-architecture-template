package org.example.project.feature.splash

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext

class DefaultSplashComponentTest {
    @Test
    fun `the splash routes to home once`() = runLifecycleTest { lifecycle ->
        val routes = mutableListOf<String>()

        DefaultSplashComponent(
                componentContext = testComponentContext(lifecycle = lifecycle),
                onNavigateToHome = { routes += "home" },
            )
            .state
        runCurrent()

        assertThat(routes).isEqualTo(listOf("home"))
    }
}
