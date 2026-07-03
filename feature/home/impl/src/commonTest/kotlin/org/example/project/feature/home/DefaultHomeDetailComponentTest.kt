package org.example.project.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.core.testing.LifecycleTestMainMode
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.home.details.presentation.DefaultHomeDetailComponent
import org.example.project.feature.home.details.presentation.HomeDetailComponent

class DefaultHomeDetailComponentTest {
    @Test
    fun `produces state with correct title and description for item id`() =
        runLifecycleTest { lifecycle ->
            val component = createComponent(lifecycle = lifecycle, itemId = 42)

            component.state.test {
                val state = awaitItem()
                assertThat(state.id).isEqualTo(42)
                assertThat(state.title).isEqualTo("Item 42")
                assertThat(state.description)
                    .isEqualTo("This is the detailed description for item 42.")
            }
        }

    @Test
    fun `back click triggers navigation callback`() =
        runLifecycleTest(mainMode = LifecycleTestMainMode.Eager) { lifecycle ->
            val backClicked = MutableStateFlow(false)
            val component =
                createComponent(lifecycle = lifecycle, onBack = { backClicked.value = true })

            component.state.test {
                awaitItem().eventSink(HomeDetailComponent.Event.BackClick)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(backClicked.value).isTrue()
        }

    private fun createComponent(
        lifecycle: LifecycleRegistry,
        itemId: Int = 1,
        onBack: () -> Unit = {},
    ): HomeDetailComponent {
        val context = testComponentContext(lifecycle = lifecycle)

        return DefaultHomeDetailComponent(
            componentContext = context,
            itemId = itemId,
            onBack = onBack,
        )
    }
}
