package org.example.project.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable actual fun rememberIsSystemInDarkMode(): Boolean = isSystemInDarkTheme()
