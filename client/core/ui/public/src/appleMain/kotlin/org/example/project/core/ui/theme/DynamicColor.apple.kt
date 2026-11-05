package org.example.project.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual val isDynamicColorAvailable: Boolean = false

// Never surfaced — dynamic color is unavailable on Apple targets, so the toggle stays hidden.
actual val dynamicColorSource: DynamicColorSource = DynamicColorSource.SystemAccent

@Composable actual fun rememberDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
