package org.example.project.feature.settings

import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.core.screenshot.testing.ScreenshotTest
import org.junit.Test

class SettingsScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun settingsDefault() {
        capture {
            SettingsScreenContent(
                state = SettingsComponent.State(themeMode = ThemeMode.System, eventSink = {}),
            )
        }
    }

    @Test
    fun settingsLogoutFailed() {
        capture {
            SettingsScreenContent(
                state =
                    SettingsComponent.State(
                        themeMode = ThemeMode.Dark,
                        logoutFailed = true,
                        eventSink = {},
                    ),
            )
        }
    }
}
