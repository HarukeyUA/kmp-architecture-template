package org.example.project.feature.tasks

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.EmbeddedComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.error.AppError

/** The list pane. Its input is the id of the open task, which the list marks as selected. */
interface TaskListComponent : EmbeddedComponent<String?, TaskListComponent.State> {

    sealed interface State : UiState {
        /** Nothing cached yet, and the first answer from the server is still on its way. */
        data object Loading : State

        sealed interface Loaded : State {
            val eventSink: (Event) -> Unit
        }

        /** The server says this account has no tasks. */
        data class Empty(override val eventSink: (Event) -> Unit) : Loaded

        /** Nothing cached and the server did not answer, so this is not known to be empty. */
        data class Failed(val error: AppError, override val eventSink: (Event) -> Unit) : Loaded

        data class Content(
            val tasks: List<Task>,
            val selectedTaskId: String?,
            /** Tasks with a done change on its way to the server. */
            val updatingTaskIds: Set<String>,
            val isRefreshing: Boolean,
            /** The last refresh or done change that failed; the cached list is still good. */
            val error: AppError?,
            override val eventSink: (Event) -> Unit,
        ) : Loaded
    }

    sealed interface Event : UiEvent {
        data class TaskClicked(val id: String) : Event

        data class DoneToggled(val id: String, val done: Boolean) : Event

        data object AddClicked : Event

        data object RefreshClicked : Event

        data object ErrorDismissed : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            onTaskClick: (String) -> Unit,
            onAddClick: () -> Unit,
        ): TaskListComponent
    }
}
