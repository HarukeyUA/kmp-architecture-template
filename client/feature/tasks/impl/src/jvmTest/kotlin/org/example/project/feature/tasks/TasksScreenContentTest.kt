package org.example.project.feature.tasks

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.core.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
class TasksScreenContentTest {
    private val listEvents = mutableListOf<TaskListComponent.Event>()
    private val detailEvents = mutableListOf<TaskDetailComponent.Event>()

    private val list =
        TaskListComponent.State.Content(
            tasks = previewTasks,
            selectedTaskId = null,
            updatingTaskIds = setOf(previewTasks[2].id),
            isRefreshing = false,
            error = null,
            eventSink = { listEvents += it },
        )

    private val detail =
        TaskDetailComponent.State.Loaded(
            task = previewTasks.first(),
            isWriting = false,
            error = null,
            eventSink = { detailEvents += it },
        )

    @Test
    fun `a compact window with no task open shows only the list`() = runComposeUiTest {
        setContent {
            AppTheme {
                TasksScreenContent(state = TasksComponent.State(list = list), isExpanded = false)
            }
        }

        onNodeWithTag(TasksScreenTestTags.LIST).assertExists()
        onNodeWithTag(TasksScreenTestTags.DETAIL).assertDoesNotExist()
    }

    @Test
    fun `an open task covers the list on a compact window`() = runComposeUiTest {
        setContent {
            AppTheme {
                TasksScreenContent(
                    state = TasksComponent.State(list = list, detail = detail),
                    isExpanded = false,
                )
            }
        }

        onNodeWithTag(TasksScreenTestTags.LIST).assertDoesNotExist()
        onNodeWithTag(TasksScreenTestTags.DETAIL_TITLE).assertExists()
    }

    @Test
    fun `a wide window shows both panes with a placeholder until a task opens`() =
        runComposeUiTest {
            setContent {
                AppTheme {
                    TasksScreenContent(state = TasksComponent.State(list = list), isExpanded = true)
                }
            }

            onNodeWithTag(TasksScreenTestTags.LIST).assertExists()
            onNodeWithTag(TasksScreenTestTags.DETAIL_PLACEHOLDER).assertExists()
        }

    @Test
    fun `a wide window shows the open task beside the list`() = runComposeUiTest {
        setContent {
            AppTheme {
                TasksScreenContent(
                    state = TasksComponent.State(list = list, detail = detail),
                    isExpanded = true,
                )
            }
        }

        onNodeWithTag(TasksScreenTestTags.LIST).assertExists()
        onNodeWithTag(TasksScreenTestTags.DETAIL_TITLE).assertExists()
    }

    @Test
    fun `the row body opens and its checkbox toggles, without doing each other's job`() =
        runComposeUiTest {
            val task = previewTasks.first()
            setContent { AppTheme { TaskListPane(state = list) } }

            onNodeWithTag(TasksScreenTestTags.rowDone(task.title)).performClick()
            assertThat(listEvents.toList())
                .containsExactly(TaskListComponent.Event.DoneToggled(task.id, done = true))

            listEvents.clear()
            onNodeWithTag(TasksScreenTestTags.row(task.title)).performClick()
            assertThat(listEvents.toList())
                .containsExactly(TaskListComponent.Event.TaskClicked(task.id))
        }

    @Test
    fun `a row with its done change on the way cannot be toggled again`() = runComposeUiTest {
        setContent { AppTheme { TaskListPane(state = list) } }

        onNodeWithTag(TasksScreenTestTags.rowDone(previewTasks[2].title)).assertIsNotEnabled()
        onNodeWithTag(TasksScreenTestTags.rowDone(previewTasks[0].title)).assertIsEnabled()
    }

    @Test
    fun `the detail switch and delete send their events`() = runComposeUiTest {
        setContent { AppTheme { TaskDetailPane(state = detail, showBack = true) } }

        onNodeWithTag(TasksScreenTestTags.DETAIL_DONE).performClick()
        onNodeWithTag(TasksScreenTestTags.DETAIL_DELETE).performClick()

        assertThat(detailEvents.toList())
            .containsExactly(
                TaskDetailComponent.Event.DoneToggled(done = true),
                TaskDetailComponent.Event.DeleteClicked,
            )
    }

    @Test
    fun `the detail actions lock while a write is on its way`() = runComposeUiTest {
        setContent {
            AppTheme {
                TaskDetailPane(state = detail.copy(isWriting = true), showBack = true)
            }
        }

        onNodeWithTag(TasksScreenTestTags.DETAIL_DONE).assertIsNotEnabled()
        onNodeWithTag(TasksScreenTestTags.DETAIL_DELETE).assertIsNotEnabled()
    }

    @Test
    fun `the form sends what was typed through its state`() = runComposeUiTest {
        val title = TextFieldState()
        var submits = 0
        setContent {
            AppTheme {
                CreateTaskForm(
                    state =
                        CreateTaskComponent.State(
                            title = title,
                            eventSink = {
                                if (it == CreateTaskComponent.Event.SubmitClicked) submits += 1
                            },
                        ),
                    onCancel = {},
                )
            }
        }

        onNodeWithTag(TasksScreenTestTags.CREATE_TITLE).performTextInput("Buy milk")
        onNodeWithTag(TasksScreenTestTags.CREATE_SUBMIT).performClick()

        assertThat(title.text.toString()).isEqualTo("Buy milk")
        assertThat(submits).isEqualTo(1)
    }
}
