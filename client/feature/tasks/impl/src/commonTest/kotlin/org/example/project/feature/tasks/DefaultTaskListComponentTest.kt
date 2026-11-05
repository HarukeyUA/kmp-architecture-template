package org.example.project.feature.tasks

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.testing.EmbeddedComponentTestHost
import org.example.project.core.testing.asBranch
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.tasks.TaskListComponent.Event
import org.example.project.feature.tasks.TaskListComponent.State
import org.example.project.shared.tasks.TaskNotFound

class DefaultTaskListComponentTest {
    private val offline = CallFailure.Transport(NetworkError.Connection(IllegalStateException("x")))
    private val clicks = mutableListOf<String>()
    private var addClicks = 0

    @Test
    fun `an empty cache waits for the first refresh`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle, FakeTasksRepository())

        assertThat(host.state.value).isEqualTo(State.Loading)
    }

    @Test
    fun `the server saying there is nothing shows the empty state`() =
        runLifecycleTest { lifecycle ->
            val host = host(lifecycle, FakeTasksRepository())
            runCurrent()

            host.state.value.asBranch<State.Empty>()
        }

    @Test
    fun `an empty cache with no connection is a failure, not an empty list`() =
        runLifecycleTest { lifecycle ->
            val repository = FakeTasksRepository().apply { nextRefreshError = offline }
            val host = host(lifecycle, repository)
            runCurrent()

            assertThat(host.state.value.asBranch<State.Failed>().error).isEqualTo(offline)
        }

    @Test
    fun `retry recovers from a failed first load`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository().apply { nextRefreshError = offline }
        val host = host(lifecycle, repository)
        runCurrent()

        repository.nextRefreshError = null
        repository.serverTasks = listOf(TASK)
        host.state.value.asBranch<State.Failed>().eventSink(Event.RefreshClicked)
        runCurrent()

        assertThat(host.state.value.asBranch<State.Content>().tasks).containsExactly(TASK)
    }

    @Test
    fun `cached tasks stay readable with no connection, and the failure is reported`() =
        runLifecycleTest { lifecycle ->
            val repository = FakeTasksRepository(listOf(TASK)).apply { nextRefreshError = offline }
            val host = host(lifecycle, repository)
            runCurrent()

            val state = host.state.value.asBranch<State.Content>()
            assertThat(state.tasks).containsExactly(TASK)
            assertThat(state.error).isEqualTo(offline)
        }

    @Test
    fun `opening refreshes once and the button refreshes again`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        val host = host(lifecycle, repository)
        runCurrent()
        assertThat(repository.refreshCalls).isEqualTo(1)

        host.state.value.asBranch<State.Content>().eventSink(Event.RefreshClicked)
        runCurrent()

        assertThat(repository.refreshCalls).isEqualTo(2)
    }

    @Test
    fun `the input marks the open task`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle, FakeTasksRepository(listOf(TASK)))
        runCurrent()
        assertThat(host.state.value.asBranch<State.Content>().selectedTaskId).isNull()

        host.setInput(TASK.id)
        runCurrent()

        assertThat(host.state.value.asBranch<State.Content>().selectedTaskId).isEqualTo(TASK.id)
    }

    @Test
    fun `row and add clicks go to the owner`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle, FakeTasksRepository(listOf(TASK)))
        runCurrent()
        val sink = host.state.value.asBranch<State.Content>().eventSink

        sink(Event.TaskClicked(TASK.id))
        sink(Event.AddClicked)

        assertThat(clicks).containsExactly(TASK.id)
        assertThat(addClicks).isEqualTo(1)
    }

    @Test
    fun `a done change shows up through the cache`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        val host = host(lifecycle, repository)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate

        host.currentContent().eventSink(Event.DoneToggled(TASK.id, true))
        runCurrent()
        assertThat(host.currentContent().updatingTaskIds).isEqualTo(setOf(TASK.id))

        gate.complete(Unit)
        runCurrent()

        val state = host.currentContent()
        assertThat(state.tasks.single().done).isEqualTo(true)
        assertThat(state.updatingTaskIds).isEmpty()
    }

    @Test
    fun `a done change that failed keeps the row and reports the failure`() =
        runLifecycleTest { lifecycle ->
            val repository = FakeTasksRepository(listOf(TASK))
            val host = host(lifecycle, repository)
            runCurrent()

            repository.nextSetDoneError = CallFailure.Declared(TaskNotFound)
            host.currentContent().eventSink(Event.DoneToggled(TASK.id, true))
            runCurrent()

            val state = host.currentContent()
            assertThat(state.tasks.single().done).isEqualTo(false)
            assertThat(state.error).isEqualTo(CallFailure.Declared(TaskNotFound))
            assertThat(state.updatingTaskIds).isEmpty()
        }

    @Test
    fun `a reported failure can be dismissed`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK)).apply { nextRefreshError = offline }
        val host = host(lifecycle, repository)
        runCurrent()

        host.currentContent().eventSink(Event.ErrorDismissed)
        runCurrent()

        assertThat(host.currentContent().error).isNull()
    }

    private fun EmbeddedComponentTestHost<String?, State>.currentContent(): State.Content =
        state.value.asBranch()

    private fun host(
        lifecycle: LifecycleRegistry,
        repository: TasksRepository,
    ): EmbeddedComponentTestHost<String?, State> {
        val child =
            DefaultTaskListComponent(
                componentContext = testComponentContext(lifecycle),
                onTaskClick = { clicks += it },
                onAddClick = { addClicks += 1 },
                repository = repository,
            )
        return EmbeddedComponentTestHost(testComponentContext(lifecycle), null, child).apply {
            state
        }
    }

    private companion object {
        val TASK = Task("1", "Book the dentist", done = false, Instant.fromEpochSeconds(1))
    }
}
