package org.example.project.feature.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.Loadable
import org.example.project.core.component.MoleculeEmbeddedComponent
import org.example.project.core.error.AppError
import org.example.project.feature.tasks.TaskDetailComponent.Event
import org.example.project.feature.tasks.TaskDetailComponent.State

@AssistedInject
class DefaultTaskDetailComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val taskId: String,
    @Assisted private val onClose: () -> Unit,
    private val repository: TasksRepository,
) : TaskDetailComponent, MoleculeEmbeddedComponent<Unit, State>(componentContext) {

    @Composable
    override fun produceState(input: Unit): State {
        val read by remember {
            observeRead()
        }
            .collectAsStateWithLifecycle<Loadable<Task?>>(initialValue = Loadable.Loading)
        var isWriting by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<AppError?>(null) }

        // Picks up a change made on another device. Offline the cached copy simply stays on screen,
        // and the outcome is not reported: this screen was not asked to do anything yet.
        LaunchedEffect(Unit) { repository.refresh(taskId) }

        fun write(action: suspend () -> AppError?, onSuccess: () -> Unit = {}) {
            if (isWriting) return
            Snapshot.withMutableSnapshot {
                isWriting = true
                error = null
            }
            scope.launch {
                val failure = action()
                Snapshot.withMutableSnapshot {
                    isWriting = false
                    error = failure
                }
                if (failure == null) onSuccess()
            }
        }

        val task =
            when (val current = read) {
                is Loadable.Failed,
                Loadable.Loading -> return State.Loading
                is Loadable.Loaded -> current.data
            }

        return if (task == null) {
            State.Missing(eventSink = { onClose() })
        } else {
            State.Loaded(
                task = task,
                isWriting = isWriting,
                error = error,
                eventSink = { event ->
                    when (event) {
                        Event.CloseClicked -> onClose()
                        is Event.DoneToggled ->
                            write({ repository.setDone(taskId, event.done).leftOrNull() })
                        Event.DeleteClicked ->
                            write({ repository.delete(taskId).leftOrNull() }, onSuccess = onClose)
                    }
                },
            )
        }
    }

    private fun observeRead(): Flow<Loadable<Task?>> =
        repository.observeTask(taskId).map { Loadable.Loaded(it) }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : TaskDetailComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            taskId: String,
            onClose: () -> Unit,
        ): DefaultTaskDetailComponent
    }
}
