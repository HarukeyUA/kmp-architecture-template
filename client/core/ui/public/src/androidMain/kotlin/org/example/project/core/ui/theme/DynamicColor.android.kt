package org.example.project.core.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual val isDynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual val dynamicColorSource: DynamicColorSource = DynamicColorSource.Wallpaper

@Composable
actual fun rememberDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (!isDynamicColorAvailable) return null
    val context = LocalContext.current
    return remember(context, darkTheme) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
}
