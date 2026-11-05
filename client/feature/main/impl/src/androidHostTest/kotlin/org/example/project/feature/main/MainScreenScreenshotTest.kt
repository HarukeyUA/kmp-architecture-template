package org.example.project.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.example.project.core.screenshot.testing.ScreenshotTest
import org.junit.Test

class MainScreenScreenshotTest : ScreenshotTest() {
    @Test
    fun mainBottomBar() {
        capture {
            MainScreenContent(
                state = MainComponent.State(eventSink = {}),
                activeTab = MainComponent.Tab.Tasks,
                isExpanded = false,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {}
            }
        }
    }

    @Test
    fun mainRail() {
        capture {
            MainScreenContent(
                state = MainComponent.State(eventSink = {}),
                activeTab = MainComponent.Tab.Settings,
                isExpanded = true,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {}
            }
        }
    }
}
