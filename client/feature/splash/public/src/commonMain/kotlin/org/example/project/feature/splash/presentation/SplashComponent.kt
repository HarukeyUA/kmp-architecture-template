package org.example.project.feature.splash.presentation

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

/** The cold-start hold: nothing else shows until the stored session has answered. */
interface SplashComponent : StatefulComponent<SplashComponent.State> {

    sealed interface State : UiState {
        data object Loading : State

        /** The stored session did not answer. It may still be there, so nothing was signed out. */
        data class SessionUnreadable(val eventSink: (Event) -> Unit) : State
    }

    sealed interface Event : UiEvent {
        data object RetryClicked : Event

        /** Gives up on the stored session, which is the only way past one that stays unreadable. */
        data object SignInAgainClicked : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            onNavigateToMain: () -> Unit,
            onNavigateToLogin: () -> Unit,
        ): SplashComponent
    }
}
