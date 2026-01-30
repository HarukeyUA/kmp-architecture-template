package org.example.project.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import com.arkivanov.decompose.ComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface SearchComponent : StatefulComponent<SearchComponent.State, SearchComponent.Event> {
    data class State(
        val queryTextFieldState: TextFieldState = TextFieldState(),
        val results: List<String> = emptyList(),
        val isSearching: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object SearchClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: ComponentContext): SearchComponent
    }
}
