package org.example.project.feature.home

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

/** The signed-in landing screen: a placeholder for the app's content, with a way to sign out. */
interface HomeComponent : StatefulComponent<HomeComponent.State> {
    data class State(val eventSink: (Event) -> Unit) : UiState

    sealed interface Event : UiEvent {
        data object LogoutClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext, onLogout: () -> Unit): HomeComponent
    }
}
