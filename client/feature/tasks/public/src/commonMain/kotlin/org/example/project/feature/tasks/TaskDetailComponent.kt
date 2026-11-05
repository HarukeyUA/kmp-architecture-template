package org.example.project.feature.tasks

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.EmbeddedComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.error.AppError

/** One task: its details, the done switch and delete. */
interface TaskDetailComponent : EmbeddedComponent<Unit, TaskDetailComponent.State> {

    sealed interface State : UiState {
        data object Loading : State

        /** The cache has no such task, for example after it was deleted on another device. */
        data class Missing(val eventSink: (Event.CloseClicked) -> Unit) : State

        data class Loaded(
            val task: Task,
            /** A done change or delete is on its way to the server. */
            val isWriting: Boolean,
            val error: AppError?,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    sealed interface Event : UiEvent {
        data object CloseClicked : Event

        data class DoneToggled(val done: Boolean) : Event

        data object DeleteClicked : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            taskId: String,
            onClose: () -> Unit,
        ): TaskDetailComponent
    }
}
