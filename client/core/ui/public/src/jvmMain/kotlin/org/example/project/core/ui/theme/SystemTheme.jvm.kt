package org.example.project.core.ui.theme

import androidx.compose.runtime.Composable
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode

@Composable actual fun rememberIsSystemInDarkMode(): Boolean = isSystemInDarkMode()
