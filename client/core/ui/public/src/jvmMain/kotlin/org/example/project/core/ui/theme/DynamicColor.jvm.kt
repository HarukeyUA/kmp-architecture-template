package org.example.project.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.materialkolor.dynamicColorScheme
import dev.nucleusframework.systemcolor.isSystemAccentColorSupported
import dev.nucleusframework.systemcolor.systemAccentColor

// `isSystemAccentColorSupported()` triggers a JNI native lib load, so it waits for the first read.
actual val isDynamicColorAvailable: Boolean by lazy { isSystemAccentColorSupported() }

actual val dynamicColorSource: DynamicColorSource = DynamicColorSource.SystemAccent

@Composable
actual fun rememberDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    // `null` covers unsupported platforms and macOS multicolor mode: no seed color, so callers
    // fall back to the static palette in `Theme.kt`.
    val accent = systemAccentColor() ?: return null
    return dynamicColorScheme(seedColor = accent, isDark = darkTheme, isAmoled = false)
}
