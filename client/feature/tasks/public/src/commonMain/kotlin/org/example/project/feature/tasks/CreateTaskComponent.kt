package org.example.project.feature.tasks

import androidx.compose.foundation.text.input.TextFieldState
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.EmbeddedComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.error.AppError

/** The create form, shown over the list. */
interface CreateTaskComponent : EmbeddedComponent<Unit, CreateTaskComponent.State> {

    data class State(
        val title: TextFieldState,
        val titleError: TitleError? = null,
        /** A failure that is not about the title, such as no connection. */
        val error: AppError? = null,
        val isSubmitting: Boolean = false,
        /** The task exists. The UI plays its closing animation and then sends [Event.Dismissed]. */
        val isCreated: Boolean = false,
        val eventSink: (Event) -> Unit,
    ) : UiState

    enum class TitleError {
        Blank,
        TooLong,
        Invalid,
    }

    sealed interface Event : UiEvent {
        data object SubmitClicked : Event

        data object Dismissed : Event
    }

    fun interface Factory {
        fun create(
            componentContext: AppComponentContext,
            onDismiss: () -> Unit,
        ): CreateTaskComponent
    }
}
