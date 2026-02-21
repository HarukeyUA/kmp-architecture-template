package org.example.project.core.screenshot.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import org.example.project.core.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val testName = TestName()

    @Before
    fun before() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
    }

    fun capture(name: String = testName.methodName, content: @Composable () -> Unit) {
        val devices =
            listOf(
                "phone" to RobolectricDeviceQualifiers.Pixel9,
                "tablet" to RobolectricDeviceQualifiers.PixelTablet,
            )

        devices.forEach { (description, device) ->
            RuntimeEnvironment.setQualifiers(device)

            val darkModeValues = listOf(false, true)

            var darkMode by mutableStateOf(false)

            composeTestRule.setContent {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    AppTheme(darkTheme = darkMode) { key(darkMode) { content() } }
                }
            }

            darkModeValues.forEach { isDarkMode ->
                darkMode = isDarkMode
                val darkModeDesc = if (isDarkMode) "dark" else "light"

                composeTestRule
                    .onRoot()
                    .captureRoboImage(
                        filePath =
                            "src/androidHostTest/screenshots/${name}_${description}_${darkModeDesc}.png"
                    )
            }
        }
    }
}
