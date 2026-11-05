package org.example.project.feature.home

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext

class DefaultHomeComponentTest {
    @Test
    fun `home produces its state`() = runLifecycleTest { lifecycle ->
        val component = DefaultHomeComponent(testComponentContext(lifecycle = lifecycle))
        component.state
        runCurrent()

        assertThat(component.state.value).isEqualTo(HomeComponent.State)
    }
}
