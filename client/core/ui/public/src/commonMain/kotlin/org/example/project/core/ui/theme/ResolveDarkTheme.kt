package org.example.project.core.ui.theme

import androidx.compose.runtime.Composable
import org.example.project.core.local.storage.model.ThemeMode

@Composable
fun ThemeMode.resolveDarkTheme(): Boolean =
    when (this) {
        ThemeMode.System -> rememberIsSystemInDarkMode()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
