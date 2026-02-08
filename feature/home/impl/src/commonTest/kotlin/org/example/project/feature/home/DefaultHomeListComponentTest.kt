package org.example.project.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.core.testing.CoroutineTest
import org.example.project.core.testing.runLifecycleTest

class DefaultHomeListComponentTest : CoroutineTest() {
    @Test
    fun `produces initial list of three items`() = runLifecycleTest { lifecycle ->
        val component = createComponent(lifecycle)

        component.state.test {
            val state = awaitItem()
            assertThat(state.items).hasSize(3)
            assertThat(state.items[0].title).isEqualTo("First Item")
            assertThat(state.items[1].title).isEqualTo("Second Item")
            assertThat(state.items[2].title).isEqualTo("Third Item")
        }
    }

    @Test
    fun `item click triggers navigation callback with correct id`() =
        runLifecycleTest { lifecycle ->
            val selectedId = MutableStateFlow(-1)
            val component =
                createComponent(lifecycle = lifecycle, onItemSelected = { selectedId.value = it })

            component.state.test {
                component.onEvent(HomeListComponent.Event.ItemClick(id = 2))
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(selectedId.value).isEqualTo(2)
        }

    private fun createComponent(
        lifecycle: LifecycleRegistry,
        onItemSelected: (Int) -> Unit = {},
    ): HomeListComponent {
        val context = DefaultComponentContext(lifecycle = lifecycle)

        return DefaultHomeListComponent(componentContext = context, onItemSelected = onItemSelected)
    }
}
