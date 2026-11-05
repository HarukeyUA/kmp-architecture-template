package org.example.project.feature.home

import org.example.project.core.screenshot.testing.ScreenshotTest
import org.junit.Test

class HomeScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun homeScreenDefaultState() {
        capture { HomeScreenContent() }
    }
}
