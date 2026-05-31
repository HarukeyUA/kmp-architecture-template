package org.example.project.feature.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.error.AppError

@AssistedInject
class DefaultNotesComponent(
    @Assisted componentContext: AppComponentContext,
    private val repository: NotesRepository,
) :
    NotesComponent,
    MoleculeComponent<NotesComponent.State, NotesComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): NotesComponent.State {
        var notes by remember { mutableStateOf(emptyList<Note>()) }
        var textInput by rememberSaveable { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<AppError?>(null) }

        suspend fun reload() {
            isLoading = true
            repository
                .list()
                .fold(
                    ifLeft = { error = it },
                    ifRight = {
                        notes = it
                        error = null
                    },
                )
            isLoading = false
        }

        // Load the caller's notes once when the component first composes.
        LaunchedEffect(Unit) { reload() }

        CollectEvents { event ->
            when (event) {
                is NotesComponent.Event.TextChanged -> {
                    textInput = event.value
                    error = null
                }
                NotesComponent.Event.Refresh -> reload()
                NotesComponent.Event.AddClicked -> {
                    if (isSubmitting || textInput.isBlank()) return@CollectEvents
                    isSubmitting = true
                    error = null
                    repository
                        .create(textInput)
                        .fold(
                            ifLeft = { error = it },
                            ifRight = {
                                textInput = ""
                                reload()
                            },
                        )
                    isSubmitting = false
                }
                is NotesComponent.Event.DeleteClicked ->
                    repository
                        .delete(event.id)
                        .fold(ifLeft = { error = it }, ifRight = { reload() })
            }
        }

        return NotesComponent.State(
            notes = notes,
            textInput = textInput,
            isLoading = isLoading,
            isSubmitting = isSubmitting,
            error = error,
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : NotesComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultNotesComponent
    }
}
