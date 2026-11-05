package org.example.project.core.ui.theme

import androidx.compose.runtime.Composable

/**
 * Reactive variant of `androidx.compose.foundation.isSystemInDarkTheme()`. Android and iOS already
 * recompose when the OS theme flips; the JVM `actual` bridges to a native dark-mode listener so the
 * desktop window follows the OS without polling or restarting.
 */
@Composable expect fun rememberIsSystemInDarkMode(): Boolean
