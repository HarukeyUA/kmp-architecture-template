package org.example.project.core.ui.error

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.DefaultBinding
import dev.zacsweers.metro.ExperimentalMetroApi
import org.example.project.core.error.AppError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Renders a user-facing message for an [AppError]. Returns null if this renderer does not handle
 * the given error type.
 *
 * Each module contributes its own [ErrorRenderer] via DI multibindings.
 */
@OptIn(ExperimentalMetroApi::class)
@DefaultBinding<ErrorRenderer<AppError>>
interface ErrorRenderer<T : AppError> {
    fun resolveResource(error: T): ResourceResult?

    @Composable
    fun render(error: T): String? =
        resolveResource(error)?.let { (resource, args) -> stringResource(resource, *args) }

    suspend fun renderAsString(error: T): String? =
        resolveResource(error)?.let { (resource, args) -> getString(resource, *args) }

    data class ResourceResult(
        val resource: StringResource,
        val args: Array<out Any> = emptyArray(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ResourceResult) return false
            if (resource != other.resource) return false
            if (!args.contentEquals(other.args)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = resource.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }
}
