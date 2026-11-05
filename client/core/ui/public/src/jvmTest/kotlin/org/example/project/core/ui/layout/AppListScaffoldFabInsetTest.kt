package org.example.project.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.core.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
class AppListScaffoldFabInsetTest {

    @Test
    fun `the button clears the nav bar when no bottom bar sits below it`() = runComposeUiTest {
        setContent { AppTheme { ListWithButton(Modifier.fillMaxSize()) } }

        assertThat(gapBelowButton()).isEqualTo(NAV_BAR + FAB_SPACING)
    }

    @Test
    fun `the button ignores the nav bar a bottom bar has already consumed`() = runComposeUiTest {
        setContent {
            AppTheme {
                ListWithButton(
                    Modifier.fillMaxSize().consumeWindowInsets(PaddingValues(bottom = NAV_BAR)),
                )
            }
        }

        assertThat(gapBelowButton()).isEqualTo(FAB_SPACING)
    }

    private fun ComposeUiTest.gapBelowButton() =
        onNodeWithTag(ROOT).getUnclippedBoundsInRoot().bottom -
            onNodeWithTag(BUTTON).getUnclippedBoundsInRoot().bottom

    @Composable
    private fun ListWithButton(modifier: Modifier) {
        AppListScaffold(
            title = "Items",
            modifier = modifier.testTag(ROOT),
            windowInsets = WindowInsets(bottom = NAV_BAR),
            floatingActionButton = { Box(Modifier.size(56.dp).testTag(BUTTON)) },
        ) {}
    }

    private companion object {
        const val ROOT = "root"
        const val BUTTON = "button"
        val NAV_BAR = 48.dp
        val FAB_SPACING = 16.dp
    }
}
