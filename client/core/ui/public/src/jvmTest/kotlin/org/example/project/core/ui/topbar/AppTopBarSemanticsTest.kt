package org.example.project.core.ui.topbar

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isFalse
import kotlin.test.Test
import org.example.project.core.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
class AppTopBarSemanticsTest {

    @Test
    fun `the small bar titles the screen`() = runComposeUiTest {
        setContent { AppTheme { AppTopBar(title = "Settings") } }

        onNodeWithText("Settings").assert(isHeading())
    }

    @Test
    fun `a screen with no bar title leaves no heading behind`() = runComposeUiTest {
        setContent { AppTheme { AppTopBar(title = null) } }

        onAllNodes(isHeading()).assertCountEquals(0)
    }

    @Test
    fun `an empty bar title leaves no heading behind`() = runComposeUiTest {
        setContent { AppTheme { AppTopBar(title = "") } }

        onAllNodes(isHeading()).assertCountEquals(0)
    }

    @Test
    fun `the large bar titles the screen without promoting its subtitle`() = runComposeUiTest {
        setContent {
            AppTheme { AppLargeFlexibleTopBar(title = "Appearance", subtitle = "Theme") }
        }

        onNodeWithText("Appearance").assert(isHeading())
        val subtitle = onNodeWithText("Theme").fetchSemanticsNode()
        assertThat(subtitle.config.contains(SemanticsProperties.Heading)).isFalse()
    }
}
