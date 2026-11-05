package org.example.project.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

expect val isDynamicColorAvailable: Boolean

/** The OS setting a dynamic color scheme is seeded from — drives user-facing wording. */
enum class DynamicColorSource {
    Wallpaper,
    SystemAccent,
}

expect val dynamicColorSource: DynamicColorSource

@Composable expect fun rememberDynamicColorScheme(darkTheme: Boolean): ColorScheme?
