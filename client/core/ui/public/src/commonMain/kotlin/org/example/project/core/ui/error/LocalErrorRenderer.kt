package org.example.project.core.ui.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.example.project.client.core.ui.Res
import org.example.project.client.core.ui.error_unexpected
import org.example.project.core.error.AppError
import org.jetbrains.compose.resources.stringResource

/** Provides the app-wide [ErrorRenderer] through the composition tree. */
val LocalErrorRenderer = staticCompositionLocalOf<ErrorRenderer<AppError>> { StubErrorRenderer() }

/** Renders this [AppError] into a user-facing string using the local [ErrorRenderer]. */
@Composable
fun AppError.message(): String =
    LocalErrorRenderer.current.render(this) ?: stringResource(Res.string.error_unexpected)
