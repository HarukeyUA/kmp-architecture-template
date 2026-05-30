package org.example.project.feature.auth

import org.example.project.core.screenshot.testing.ScreenshotTest
import org.junit.Test

class LoginScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun loginScreenDefaultState() {
        capture { LoginScreenContent(state = LoginComponent.State(), onEvent = {}) }
    }
}
