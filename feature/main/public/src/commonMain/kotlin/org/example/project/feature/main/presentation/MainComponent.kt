package org.example.project.feature.main.presentation

import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.component.snackbar.SnackbarHostState
import org.example.project.core.navigation.StackComponent
import org.example.project.core.ui.navigation.ScreenChild

interface MainComponent :
    StatefulComponent<MainComponent.State>, StackComponent<MainComponent.Tab, ScreenChild> {

    val snackbarHostState: SnackbarHostState

    data class State(val eventSink: (Event) -> Unit) : UiState

    @Serializable
    sealed interface Tab {
        @Serializable data object Home : Tab

        @Serializable data object Search : Tab

        @Serializable data object Profile : Tab
    }

    sealed interface Event : UiEvent {
        data object HomeTabClick : Event

        data object SearchTabClick : Event

        data object ProfileTabClick : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext, onLogout: () -> Unit): MainComponent
    }
}
