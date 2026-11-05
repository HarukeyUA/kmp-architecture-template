package org.example.project.feature.main

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import org.example.project.core.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
class MainScreenContentTest {
    private val events = mutableListOf<MainComponent.Event>()

    @Test
    fun `a compact window gets the bottom bar`() = runComposeUiTest {
        setContent {
            AppTheme {
                MainScreenContent(
                    state = MainComponent.State(eventSink = { events += it }),
                    activeTab = MainComponent.Tab.Tasks,
                    isExpanded = false,
                ) {}
            }
        }

        onNodeWithTag(MainScreenTestTags.BOTTOM_BAR).assertExists()
        onNodeWithTag(MainScreenTestTags.RAIL).assertDoesNotExist()
        onNodeWithTag(MainScreenTestTags.TASKS_TAB).assertIsSelected()
    }

    @Test
    fun `a wide window gets the rail`() = runComposeUiTest {
        setContent {
            AppTheme {
                MainScreenContent(
                    state = MainComponent.State(eventSink = { events += it }),
                    activeTab = MainComponent.Tab.Settings,
                    isExpanded = true,
                ) {}
            }
        }

        onNodeWithTag(MainScreenTestTags.RAIL).assertExists()
        onNodeWithTag(MainScreenTestTags.BOTTOM_BAR).assertDoesNotExist()
        onNodeWithTag(MainScreenTestTags.SETTINGS_TAB).assertIsSelected()
    }

    @Test
    fun `a bottom bar tab sends its event`() = runComposeUiTest {
        tabs(isExpanded = false)

        onNodeWithTag(MainScreenTestTags.SETTINGS_TAB).performClick()

        assertThat(events.toList())
            .containsExactly(MainComponent.Event.TabClicked(MainComponent.Tab.Settings))
    }

    @Test
    fun `a rail tab sends its event`() = runComposeUiTest {
        tabs(isExpanded = true)

        onNodeWithTag(MainScreenTestTags.SETTINGS_TAB).performClick()

        assertThat(events.toList())
            .containsExactly(MainComponent.Event.TabClicked(MainComponent.Tab.Settings))
    }

    private fun ComposeUiTest.tabs(isExpanded: Boolean) {
        setContent {
            AppTheme {
                MainScreenContent(
                    state = MainComponent.State(eventSink = { events += it }),
                    activeTab = MainComponent.Tab.Tasks,
                    isExpanded = isExpanded,
                ) {}
            }
        }
    }
}
