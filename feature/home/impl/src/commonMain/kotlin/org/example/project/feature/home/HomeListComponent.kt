package org.example.project.feature.home

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface HomeListComponent : StatefulComponent<HomeListComponent.State, HomeListComponent.Event> {

    data class State(val items: List<Item> = emptyList()) : UiState

    data class Item(val id: Int, val title: String)

    sealed interface Event : UiEvent {
        data class ItemClick(val id: Int) : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            onItemSelected: (id: Int) -> Unit,
        ): HomeListComponent
    }
}
