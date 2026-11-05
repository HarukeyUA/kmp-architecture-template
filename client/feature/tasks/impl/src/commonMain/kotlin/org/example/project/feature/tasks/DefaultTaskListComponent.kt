package org.example.project.feature.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeEmbeddedComponent
import org.example.project.core.error.AppError
import org.example.project.feature.tasks.TaskListComponent.Event
import org.example.project.feature.tasks.TaskListComponent.State

@AssistedInject
class DefaultTaskListComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onTaskClick: (String) -> Unit,
    @Assisted private val onAddClick: () -> Unit,
    private val repository: TasksRepository,
) : TaskListComponent, MoleculeEmbeddedComponent<String?, State>(componentContext) {

    @Composable
    override fun produceState(input: String?): State {
        val tasks by remember {
            repository.observeTasks()
        }
            .collectAsStateWithLifecycle<List<Task>?>(initialValue = null)
        var refreshRequest by remember { mutableIntStateOf(0) }
        var isRefreshing by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<AppError?>(null) }
        var updatingTaskIds by remember { mutableStateOf(emptySet<String>()) }

        LaunchedEffect(refreshRequest) {
            Snapshot.withMutableSnapshot {
                isRefreshing = true
                error = null
            }
            val failure = repository.refresh().leftOrNull()
            Snapshot.withMutableSnapshot {
                error = failure
                isRefreshing = false
            }
        }

        val eventSink: (Event) -> Unit = { event ->
            when (event) {
                is Event.TaskClicked -> onTaskClick(event.id)
                Event.AddClicked -> onAddClick()
                Event.RefreshClicked -> if (!isRefreshing) refreshRequest += 1
                Event.ErrorDismissed -> error = null
                is Event.DoneToggled -> {
                    Snapshot.withMutableSnapshot {
                        updatingTaskIds += event.id
                        error = null
                    }
                    scope.launch {
                        val failure = repository.setDone(event.id, event.done).leftOrNull()
                        Snapshot.withMutableSnapshot {
                            updatingTaskIds -= event.id
                            if (failure != null) error = failure
                        }
                    }
                }
            }
        }

        val cached = tasks
        val failure = error
        return when {
            cached == null -> State.Loading
            cached.isNotEmpty() ->
                State.Content(
                    tasks = cached,
                    selectedTaskId = input,
                    updatingTaskIds = updatingTaskIds,
                    isRefreshing = isRefreshing,
                    error = failure,
                    eventSink = eventSink,
                )
            isRefreshing -> State.Loading
            failure != null -> State.Failed(error = failure, eventSink = eventSink)
            else -> State.Empty(eventSink = eventSink)
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : TaskListComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onTaskClick: (String) -> Unit,
            onAddClick: () -> Unit,
        ): DefaultTaskListComponent
    }
}
