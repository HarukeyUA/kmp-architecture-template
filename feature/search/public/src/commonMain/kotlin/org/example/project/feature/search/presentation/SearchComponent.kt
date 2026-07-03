package org.example.project.feature.search.presentation

import androidx.compose.foundation.text.input.TextFieldState
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState

interface SearchComponent : StatefulComponent<SearchComponent.State> {
    data class State(
        val queryTextFieldState: TextFieldState = TextFieldState(),
        val results: List<String> = emptyList(),
        val isSearching: Boolean = false,
        val eventSink: (Event) -> Unit,
    ) : UiState

    sealed interface Event : UiEvent {
        data object SearchClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext): SearchComponent
    }
}
