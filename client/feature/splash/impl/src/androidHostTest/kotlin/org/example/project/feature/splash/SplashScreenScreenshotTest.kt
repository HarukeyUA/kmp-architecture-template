package org.example.project.feature.splash

import org.example.project.core.screenshot.testing.ScreenshotTest
import org.example.project.feature.splash.presentation.SplashComponent
import org.junit.Test

class SplashScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun splashScreenLoading() {
        capture { SplashScreenContent(state = SplashComponent.State.Loading) }
    }

    @Test
    fun splashScreenSessionUnreadable() {
        capture {
            SplashScreenContent(state = SplashComponent.State.SessionUnreadable(eventSink = {}))
        }
    }
}
