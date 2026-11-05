package org.example.project.core.screenshot.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import org.example.project.core.error.AppError
import org.example.project.core.ui.error.CompositeErrorRenderer
import org.example.project.core.ui.error.ErrorRenderer
import org.example.project.core.ui.error.LocalErrorRenderer
import org.example.project.core.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
abstract class ScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val testName = TestName()

    @Before
    fun before() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    }

    /**
     * [errorRenderers] stand in for the renderers Metro contributes at runtime. A capture of an
     * error state that omits the renderer its error needs shows the generic fallback message.
     */
    fun capture(
        name: String = testName.methodName,
        errorRenderers: List<ErrorRenderer<*>> = emptyList(),
        beforeCapture: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        // CompositeErrorRenderer treats a renderer handed the wrong error type as declining it.
        @Suppress("UNCHECKED_CAST")
        val errorRenderer =
            CompositeErrorRenderer(
                errorRenderers.mapTo(mutableSetOf()) { it as ErrorRenderer<AppError> },
            )

        val devices =
            listOf(
                "phone" to RobolectricDeviceQualifiers.Pixel9,
                "tablet" to RobolectricDeviceQualifiers.PixelTablet,
            )

        devices.forEach { (description, device) ->
            listOf(false, true).forEach { isDarkMode ->
                val darkModeDesc = if (isDarkMode) "dark" else "light"

                // A composition per theme, not a flag flipped in place: a Dialog draws into its own
                // window, which keeps the colours it was built with. The rule only sets content
                // once per Activity, and a configuration change is what hands it a fresh one.
                RuntimeEnvironment.setQualifiers("+night")
                RuntimeEnvironment.setQualifiers(device)

                composeTestRule.setContent {
                    CompositionLocalProvider(
                        LocalInspectionMode provides true,
                        LocalErrorRenderer provides errorRenderer,
                    ) {
                        AppTheme(darkTheme = isDarkMode) { content() }
                    }
                }

                beforeCapture()

                composeTestRule
                    .onRoot()
                    .captureRoboImage(
                        filePath =
                            "src/androidHostTest/screenshots/${name}_${description}_${darkModeDesc}.png",
                    )
            }
        }
    }
}
