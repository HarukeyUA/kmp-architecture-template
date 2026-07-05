package org.example.project.feature.auth

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.error.AppError

interface LoginComponent : StatefulComponent<LoginComponent.State, LoginComponent.Event> {
    data class State(
        val email: String = "",
        val password: String = "",
        val isSubmitting: Boolean = false,
        val error: AppError? = null,
        val formError: FormError? = null,
    ) : UiState

    /**
     * A Declared auth failure the screen renders inline with a field-specific message, distinct
     * from the generic [error] renderer pipeline (ADR-0011): login's `InvalidCredentials`, signup's
     * `EmailTaken`. The screen owns the message; the component owns the exhaustive mapping.
     */
    enum class FormError {
        InvalidCredentials,
        EmailTaken,
    }

    sealed interface Event : UiEvent {
        data class EmailChanged(val value: String) : Event

        data class PasswordChanged(val value: String) : Event

        data object LoginClicked : Event

        data object SignupClicked : Event
    }

    fun interface Factory {
        /** [onAuthenticated] is invoked once a Session is issued (login or signup succeeded). */
        fun create(
            componentContext: AppComponentContext,
            onAuthenticated: () -> Unit,
        ): LoginComponent
    }
}
