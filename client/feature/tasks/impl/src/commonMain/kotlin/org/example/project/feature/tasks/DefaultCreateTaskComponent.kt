package org.example.project.feature.tasks

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeEmbeddedComponent
import org.example.project.core.error.AppError
import org.example.project.core.error.CallFailure
import org.example.project.feature.tasks.CreateTaskComponent.Event
import org.example.project.feature.tasks.CreateTaskComponent.State
import org.example.project.feature.tasks.CreateTaskComponent.TitleError
import org.example.project.shared.common.FieldError
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TaskValidationFailed
import org.example.project.shared.tasks.TasksCreateError

@AssistedInject
class DefaultCreateTaskComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onDismiss: () -> Unit,
    private val repository: TasksRepository,
) : CreateTaskComponent, MoleculeEmbeddedComponent<Unit, State>(componentContext) {

    @Composable
    override fun produceState(input: Unit): State {
        val title = rememberTextFieldState()
        var titleError by remember { mutableStateOf<TitleError?>(null) }
        var error by remember { mutableStateOf<AppError?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        var isCreated by remember { mutableStateOf(false) }

        // A message about the previous text stops being true as soon as the text changes.
        val text = title.text.toString()
        LaunchedEffect(text) {
            Snapshot.withMutableSnapshot {
                titleError = null
                error = null
            }
        }

        fun submit() {
            if (isSubmitting || isCreated) return
            TaskTitle.of(text)
                .fold(
                    ifLeft = { titleError = it.toTitleError() },
                    ifRight = { valid ->
                        Snapshot.withMutableSnapshot {
                            isSubmitting = true
                            titleError = null
                            error = null
                        }
                        scope.launch {
                            val failure = repository.create(valid).leftOrNull()
                            Snapshot.withMutableSnapshot {
                                isSubmitting = false
                                when (failure) {
                                    null -> isCreated = true
                                    is CallFailure.Declared ->
                                        titleError = failure.error.toTitleError()
                                    is CallFailure.Ambient,
                                    is CallFailure.Transport -> error = failure
                                }
                            }
                        }
                    },
                )
        }

        return State(
            title = title,
            titleError = titleError,
            error = error,
            isSubmitting = isSubmitting,
            isCreated = isCreated,
            eventSink = { event ->
                when (event) {
                    Event.SubmitClicked -> submit()
                    Event.Dismissed -> onDismiss()
                }
            },
        )
    }

    private fun TasksCreateError.toTitleError(): TitleError =
        when (this) {
            is TaskValidationFailed -> fields.firstOrNull()?.toTitleError() ?: TitleError.Invalid
        }

    private fun FieldError.toTitleError(): TitleError =
        when (code) {
            "blank" -> TitleError.Blank
            "too_long" -> TitleError.TooLong
            else -> TitleError.Invalid
        }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : CreateTaskComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onDismiss: () -> Unit,
        ): DefaultCreateTaskComponent
    }
}
