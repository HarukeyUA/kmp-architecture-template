package org.example.project.feature.home

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface HomeDetailComponent :
    StatefulComponent<HomeDetailComponent.State, HomeDetailComponent.Event> {

    data class State(val id: Int, val title: String, val description: String) : UiState

    sealed interface Event : UiEvent {
        data object BackClick : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            itemId: Int,
            onBack: () -> Unit,
        ): HomeDetailComponent
    }
}
