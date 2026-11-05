package org.example.project.feature.tasks

import androidx.compose.foundation.text.input.rememberTextFieldState
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.screenshot.testing.ScreenshotTest
import org.example.project.core.ui.error.NetworkErrorRenderer
import org.junit.Test

class TasksScreenScreenshotTest : ScreenshotTest() {
    private val offline = CallFailure.Transport(NetworkError.Connection(IllegalStateException("x")))

    private fun content(error: CallFailure.Transport? = null, selectedTaskId: String? = null) =
        TaskListComponent.State.Content(
            tasks = previewTasks,
            selectedTaskId = selectedTaskId,
            updatingTaskIds = emptySet(),
            isRefreshing = false,
            error = error,
            eventSink = {},
        )

    private fun detail(error: CallFailure.Transport? = null) =
        TaskDetailComponent.State.Loaded(
            task = previewTasks.first(),
            isWriting = false,
            error = error,
            eventSink = {},
        )

    // The tablet capture of each test below is the two-pane layout.
    @Test
    fun tasksList() {
        capture {
            AdaptiveTasksScreenContent(state = TasksComponent.State(list = content()))
        }
    }

    @Test
    fun tasksDetailOpen() {
        capture {
            AdaptiveTasksScreenContent(
                state =
                    TasksComponent.State(
                        list = content(selectedTaskId = previewTasks.first().id),
                        detail = detail(),
                    ),
            )
        }
    }

    @Test
    fun tasksEmpty() {
        capture {
            AdaptiveTasksScreenContent(
                state = TasksComponent.State(list = TaskListComponent.State.Empty(eventSink = {})),
            )
        }
    }

    @Test
    fun tasksLoading() {
        capture {
            AdaptiveTasksScreenContent(
                state = TasksComponent.State(list = TaskListComponent.State.Loading),
            )
        }
    }

    @Test
    fun tasksFirstLoadFailed() {
        capture(errorRenderers = listOf(NetworkErrorRenderer())) {
            AdaptiveTasksScreenContent(
                state =
                    TasksComponent.State(
                        list = TaskListComponent.State.Failed(error = offline, eventSink = {}),
                    ),
            )
        }
    }

    @Test
    fun tasksOfflineWithCache() {
        capture(errorRenderers = listOf(NetworkErrorRenderer())) {
            AdaptiveTasksScreenContent(
                state = TasksComponent.State(list = content(error = offline)),
            )
        }
    }

    @Test
    fun tasksDetailMissing() {
        capture {
            AdaptiveTasksScreenContent(
                state =
                    TasksComponent.State(
                        list = content(),
                        detail = TaskDetailComponent.State.Missing(eventSink = {}),
                    ),
            )
        }
    }

    @Test
    fun tasksDetailWriteFailed() {
        capture(errorRenderers = listOf(NetworkErrorRenderer())) {
            AdaptiveTasksScreenContent(
                state =
                    TasksComponent.State(
                        list = content(selectedTaskId = previewTasks.first().id),
                        detail = detail(error = offline),
                    ),
            )
        }
    }

    @Test
    fun tasksCreateForm() {
        capture {
            AdaptiveTasksScreenContent(
                state =
                    TasksComponent.State(
                        list = content(),
                        create =
                            CreateTaskComponent.State(
                                title = rememberTextFieldState("Buy milk"),
                                eventSink = {},
                            ),
                    ),
            )
        }
    }
}
