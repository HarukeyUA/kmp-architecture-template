package org.example.project.core.ui.error

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.error.AppError
import org.example.project.core.error.DelegatingError
import org.example.project.core.ui.Res
import org.example.project.core.ui.error_unexpected
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Delegates to registered [ErrorRenderer] instances in order. Falls back to a generic message if no
 * renderer handles the error.
 */
@Inject
@ContributesBinding(AppScope::class)
class CompositeErrorRenderer(private val renderers: Set<ErrorRenderer<AppError>>) :
    ErrorRenderer<AppError> {
    /**
     * Resolves the resource by searching through registered renderers and handling
     * [DelegatingError] recursion.
     */
    override fun resolveResource(error: AppError): ErrorRenderer.ResourceResult? {
        val result = renderers.firstNotNullOfOrNull { renderer ->
            try {
                renderer.resolveResource(error)
            } catch (_: ClassCastException) {
                null
            }
        }

        if (result != null) return result

        if (error is DelegatingError) {
            return resolveResource(error.delegate)
        }

        return null
    }

    @Composable
    override fun render(error: AppError): String {
        return super.render(error) ?: stringResource(Res.string.error_unexpected)
    }

    override suspend fun renderAsString(error: AppError): String {
        return super.renderAsString(error) ?: getString(Res.string.error_unexpected)
    }
}
