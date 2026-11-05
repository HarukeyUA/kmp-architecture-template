package org.example.project.feature.tasks

import app.cash.molecule.SnapshotNotifier
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.component.DefaultAppComponentContext
import org.example.project.core.testing.asBranch
import org.example.project.core.testing.runLifecycleTest

class DefaultTasksComponentTest {
    private val repository = FakeTasksRepository(listOf(TASK))
    private val backDispatcher = BackDispatcher()

    @Test
    fun `starts on the list with nothing open`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()

        val state = component.state.value
        state.list.asBranch<TaskListComponent.State.Content>()
        assertThat(state.detail).isNull()
        assertThat(state.create).isNull()
    }

    @Test
    fun `a row click opens the task and marks it in the list`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()

        component.list().eventSink(TaskListComponent.Event.TaskClicked(TASK.id))
        runCurrent()

        val state = component.state.value
        assertThat(state.detail.asBranch<TaskDetailComponent.State.Loaded>().task).isEqualTo(TASK)
        assertThat(component.list().selectedTaskId).isEqualTo(TASK.id)
    }

    @Test
    fun `closing the task returns to the list alone`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        component.list().eventSink(TaskListComponent.Event.TaskClicked(TASK.id))
        runCurrent()

        component.state.value.detail
            .asBranch<TaskDetailComponent.State.Loaded>()
            .eventSink(TaskDetailComponent.Event.CloseClicked)
        runCurrent()

        assertThat(component.state.value.detail).isNull()
        assertThat(component.list().selectedTaskId).isNull()
    }

    @Test
    fun `a second task replaces the first`() = runLifecycleTest { lifecycle ->
        val other = TASK.copy(id = "2", title = "Other")
        val component = component(lifecycle, FakeTasksRepository(listOf(TASK, other)))
        runCurrent()

        component.list().eventSink(TaskListComponent.Event.TaskClicked(TASK.id))
        runCurrent()
        component.list().eventSink(TaskListComponent.Event.TaskClicked(other.id))
        runCurrent()

        val detail = component.state.value.detail.asBranch<TaskDetailComponent.State.Loaded>()
        assertThat(detail.task).isEqualTo(other)
    }

    @Test
    fun `deleting the open task closes it and drops its row`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        component.list().eventSink(TaskListComponent.Event.TaskClicked(TASK.id))
        runCurrent()

        component.state.value.detail
            .asBranch<TaskDetailComponent.State.Loaded>()
            .eventSink(TaskDetailComponent.Event.DeleteClicked)
        runCurrent()

        val state = component.state.value
        assertThat(state.detail).isNull()
        state.list.asBranch<TaskListComponent.State.Empty>()
    }

    @Test
    fun `add opens the form and dismissing closes it`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()

        component.list().eventSink(TaskListComponent.Event.AddClicked)
        runCurrent()
        val form = component.state.value.create
        assertThat(form).isNotNull()

        checkNotNull(form).eventSink(CreateTaskComponent.Event.Dismissed)
        runCurrent()
        assertThat(component.state.value.create).isNull()
    }

    @Test
    fun `back closes the form, then the task`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        component.list().eventSink(TaskListComponent.Event.TaskClicked(TASK.id))
        runCurrent()
        component.list().eventSink(TaskListComponent.Event.AddClicked)
        runCurrent()

        assertThat(backDispatcher.back()).isEqualTo(true)
        runCurrent()
        assertThat(component.state.value.create).isNull()
        assertThat(component.state.value.detail).isNotNull()

        assertThat(backDispatcher.back()).isEqualTo(true)
        runCurrent()
        assertThat(component.state.value.detail).isNull()

        assertThat(backDispatcher.back()).isEqualTo(false)
    }

    private fun TasksComponent.list(): TaskListComponent.State.Content = state.value.list.asBranch()

    private fun component(
        lifecycle: LifecycleRegistry,
        repository: TasksRepository = this.repository,
    ): TasksComponent {
        val component =
            DefaultTasksComponent(
                componentContext =
                    DefaultAppComponentContext(
                        lifecycle = lifecycle,
                        backHandler = backDispatcher,
                        snapshotNotifier = SnapshotNotifier.WhileActive,
                    ),
                listFactory = { context, onTaskClick, onAddClick ->
                    DefaultTaskListComponent(context, onTaskClick, onAddClick, repository)
                },
                detailFactory = { context, taskId, onClose ->
                    DefaultTaskDetailComponent(context, taskId, onClose, repository)
                },
                createFactory = { context, onDismiss ->
                    DefaultCreateTaskComponent(context, onDismiss, repository)
                },
            )
        component.state
        return component
    }

    private companion object {
        val TASK = Task("1", "Book the dentist", done = false, Instant.fromEpochSeconds(1))
    }
}
