package org.example.project.feature.tasks

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.testing.EmbeddedComponentTestHost
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.tasks.CreateTaskComponent.Event
import org.example.project.feature.tasks.CreateTaskComponent.State
import org.example.project.feature.tasks.CreateTaskComponent.TitleError
import org.example.project.shared.common.FieldError
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TaskValidationFailed

class DefaultCreateTaskComponentTest {
    private val repository = FakeTasksRepository()
    private var dismissals = 0

    @Test
    fun `a valid title is created trimmed and reports that it is done`() =
        runLifecycleTest { lifecycle ->
            val host = host(lifecycle)
            runCurrent()

            host.type("  Buy milk  ")
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            repository.writeGate = gate
            host.state.value.eventSink(Event.SubmitClicked)
            runCurrent()
            assertThat(host.state.value.isSubmitting).isTrue()

            gate.complete(Unit)
            runCurrent()

            assertThat(repository.createdTitles).containsExactly("Buy milk")
            assertThat(host.state.value.isCreated).isTrue()
            assertThat(host.state.value.isSubmitting).isFalse()
            assertThat(dismissals).isEqualTo(0)
        }

    @Test
    fun `a blank title never reaches the server`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle)
        runCurrent()

        host.state.value.eventSink(Event.SubmitClicked)
        runCurrent()

        assertThat(host.state.value.titleError).isEqualTo(TitleError.Blank)
        assertThat(repository.createdTitles).isEmpty()
    }

    @Test
    fun `an overlong title never reaches the server`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle)
        runCurrent()

        host.type("x".repeat(TaskTitle.MAX_LENGTH + 1))
        runCurrent()
        host.state.value.eventSink(Event.SubmitClicked)
        runCurrent()

        assertThat(host.state.value.titleError).isEqualTo(TitleError.TooLong)
        assertThat(repository.createdTitles).isEmpty()
    }

    @Test
    fun `the server's validation failure lands on the field`() = runLifecycleTest { lifecycle ->
        repository.nextCreateError =
            CallFailure.Declared(TaskValidationFailed(listOf(FieldError("title", "too_long"))))
        val host = host(lifecycle)
        runCurrent()

        host.type("Buy milk")
        runCurrent()
        host.state.value.eventSink(Event.SubmitClicked)
        runCurrent()

        val state = host.state.value
        assertThat(state.titleError).isEqualTo(TitleError.TooLong)
        assertThat(state.error).isNull()
        assertThat(state.isCreated).isFalse()
    }

    @Test
    fun `no connection fails visibly and keeps the form open with its text`() =
        runLifecycleTest { lifecycle ->
            val offline =
                CallFailure.Transport(NetworkError.Connection(IllegalStateException("offline")))
            repository.nextCreateError = offline
            val host = host(lifecycle)
            runCurrent()

            host.type("Buy milk")
            runCurrent()
            host.state.value.eventSink(Event.SubmitClicked)
            runCurrent()

            val state = host.state.value
            assertThat(state.error).isEqualTo(offline)
            assertThat(state.isCreated).isFalse()
            assertThat(state.title.text.toString()).isEqualTo("Buy milk")
        }

    @Test
    fun `typing clears the message about the previous text`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle)
        runCurrent()
        host.state.value.eventSink(Event.SubmitClicked)
        runCurrent()
        assertThat(host.state.value.titleError).isEqualTo(TitleError.Blank)

        host.type("B")
        runCurrent()

        assertThat(host.state.value.titleError).isNull()
    }

    @Test
    fun `dismissing goes to the owner`() = runLifecycleTest { lifecycle ->
        val host = host(lifecycle)
        runCurrent()

        host.state.value.eventSink(Event.Dismissed)

        assertThat(dismissals).isEqualTo(1)
    }

    private fun EmbeddedComponentTestHost<Unit, State>.type(text: String) {
        Snapshot.withMutableSnapshot { state.value.title.setTextAndPlaceCursorAtEnd(text) }
    }

    private fun host(lifecycle: LifecycleRegistry): EmbeddedComponentTestHost<Unit, State> {
        val child =
            DefaultCreateTaskComponent(
                componentContext = testComponentContext(lifecycle),
                onDismiss = { dismissals += 1 },
                repository = repository,
            )
        return EmbeddedComponentTestHost(testComponentContext(lifecycle), Unit, child).apply {
            state
        }
    }
}
