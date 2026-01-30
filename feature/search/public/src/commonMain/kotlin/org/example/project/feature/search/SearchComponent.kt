package org.example.project.feature.search

import com.arkivanov.decompose.ComponentContext
import kotlinx.serialization.Serializable
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface SearchComponent : StatefulComponent<SearchComponent.State, SearchComponent.Event> {
    @Serializable
    data class State(
        val query: String = "",
        val results: List<String> = emptyList(),
        val isSearching: Boolean = false
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data object SearchClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext): SearchComponent
    }
}
