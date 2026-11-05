package org.example.project.feature.main

import kotlinx.serialization.Serializable
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.navigation.StackComponent
import org.example.project.core.ui.navigation.ScreenChild

/** The signed-in shell: the tabs and whichever tab is open. */
interface MainComponent :
    StatefulComponent<MainComponent.State>, StackComponent<MainComponent.Tab, ScreenChild> {

    data class State(val eventSink: (Event) -> Unit) : UiState

    @Serializable
    sealed interface Tab {
        @Serializable data object Tasks : Tab

        @Serializable data object Settings : Tab
    }

    sealed interface Event : UiEvent {
        data class TabClicked(val tab: Tab) : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext): MainComponent
    }
}
