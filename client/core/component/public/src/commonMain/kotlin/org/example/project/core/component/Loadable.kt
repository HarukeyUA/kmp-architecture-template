package org.example.project.core.component

import org.example.project.core.error.AppError

/** Models the three mutually exclusive states of a loadable resource. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Loaded<T>(val data: T) : Loadable<T>

    data class Failed(val error: AppError) : Loadable<Nothing>
}
