package org.example.project.feature.notes

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.error.AppError
import org.example.project.shared.notes.NotesQuotaExceeded

interface NotesComponent : StatefulComponent<NotesComponent.State> {
    data class State(
        val notes: List<Note> = emptyList(),
        val textInput: String = "",
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: AppError? = null,
        // The one Declared error `create` can return (ADR-0011), rendered inline with its budget;
        // anything else rides the generic [error] pipeline.
        val quotaExceeded: NotesQuotaExceeded? = null,
        val eventSink: (Event) -> Unit,
    ) : UiState

    sealed interface Event : UiEvent {
        data class TextChanged(val value: String) : Event

        data object AddClicked : Event

        data class DeleteClicked(val id: String) : Event

        data object Refresh : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext): NotesComponent
    }
}
