package org.example.project.feature.tasks

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
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
import org.example.project.feature.tasks.TaskDetailComponent.Event
import org.example.project.feature.tasks.TaskDetailComponent.State

class DefaultTaskDetailComponentTest {
    private val offline = CallFailure.Transport(NetworkError.Connection(IllegalStateException("x")))
    private var closes = 0

    @Test
    fun `the cached task shows, and the server is asked for a fresher one`() =
        runLifecycleTest { lifecycle ->
            val repository = FakeTasksRepository(listOf(TASK))
            val host = host(lifecycle, repository)
            runCurrent()

            assertThat(host.state.value.asBranch<State.Loaded>().task).isEqualTo(TASK)
            assertThat(repository.refreshedTaskIds).containsExactly(TASK.id)
        }

    @Test
    fun `offline the cached task stays with nothing reported`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        repository.nextRefreshTaskError = offline
        val host = host(lifecycle, repository)
        runCurrent()

        val state = host.state.value.asBranch<State.Loaded>()
        assertThat(state.task).isEqualTo(TASK)
        assertThat(state.error).isNull()
    }

    @Test
    fun `a task the cache does not have reads as missing`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle, FakeTasksRepository())
        runCurrent()

        host.state.value.asBranch<State.Missing>().eventSink(Event.CloseClicked)

        assertThat(closes).isEqualTo(1)
    }

    @Test
    fun `done follows the cache after the write`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        val host = host(lifecycle, repository)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate

        host.state.value.asBranch<State.Loaded>().eventSink(Event.DoneToggled(done = true))
        runCurrent()
        assertThat(host.state.value.asBranch<State.Loaded>().isWriting).isTrue()

        gate.complete(Unit)
        runCurrent()

        val state = host.state.value.asBranch<State.Loaded>()
        assertThat(state.task.done).isTrue()
        assertThat(state.isWriting).isFalse()
    }

    @Test
    fun `a failed done change keeps the task and shows the failure`() =
        runLifecycleTest { lifecycle ->
            val repository = FakeTasksRepository(listOf(TASK))
            val host = host(lifecycle, repository)
            runCurrent()

            repository.nextSetDoneError = offline
            host.state.value.asBranch<State.Loaded>().eventSink(Event.DoneToggled(done = true))
            runCurrent()

            val state = host.state.value.asBranch<State.Loaded>()
            assertThat(state.task.done).isFalse()
            assertThat(state.error).isEqualTo(offline)
        }

    @Test
    fun `delete closes the detail after the write`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        val host = host(lifecycle, repository)
        runCurrent()

        host.state.value.asBranch<State.Loaded>().eventSink(Event.DeleteClicked)
        runCurrent()

        assertThat(repository.current).isEmpty()
        assertThat(closes).isEqualTo(1)
    }

    @Test
    fun `a failed delete stays open and shows the failure`() = runLifecycleTest { lifecycle ->
        val repository = FakeTasksRepository(listOf(TASK))
        val host = host(lifecycle, repository)
        runCurrent()

        repository.nextDeleteError = offline
        host.state.value.asBranch<State.Loaded>().eventSink(Event.DeleteClicked)
        runCurrent()

        assertThat(closes).isEqualTo(0)
        assertThat(host.state.value).isInstanceOf(State.Loaded::class)
        assertThat(host.state.value.asBranch<State.Loaded>().error).isEqualTo(offline)
    }

    private fun host(
        lifecycle: LifecycleRegistry,
        repository: TasksRepository,
    ): EmbeddedComponentTestHost<Unit, State> {
        val child =
            DefaultTaskDetailComponent(
                componentContext = testComponentContext(lifecycle),
                taskId = TASK.id,
                onClose = { closes += 1 },
                repository = repository,
            )
        return EmbeddedComponentTestHost(testComponentContext(lifecycle), Unit, child).apply {
            state
        }
    }

    private companion object {
        val TASK = Task("1", "Book the dentist", done = false, Instant.fromEpochSeconds(1))
    }
}
