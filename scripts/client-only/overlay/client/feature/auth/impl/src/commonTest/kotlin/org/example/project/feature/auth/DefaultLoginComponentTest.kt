package org.example.project.feature.auth

import app.cash.turbine.test
import arrow.core.left
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.NetworkError
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext

class DefaultLoginComponentTest {
    @Test
    fun `email and password edits update state`() = runLifecycleTest { lifecycle ->
        val component = createComponent(lifecycle = lifecycle)

        component.state.test {
            val initial = awaitItem()
            assertThat(initial.email).isEqualTo("")
            initial.eventSink(LoginComponent.Event.EmailChanged("alice@example.com"))
            val withEmail = awaitItem()
            assertThat(withEmail.email).isEqualTo("alice@example.com")
            withEmail.eventSink(LoginComponent.Event.PasswordChanged("hunter2hunter2"))
            assertThat(awaitItem().password).isEqualTo("hunter2hunter2")
        }
    }

    @Test
    fun `successful login invokes the authenticated callback`() = runLifecycleTest { lifecycle ->
        var authenticated = false
        val component =
            createComponent(lifecycle = lifecycle, onAuthenticated = { authenticated = true })

        component.state.test {
            awaitItem().eventSink(LoginComponent.Event.LoginClicked)
            runCurrent()
            cancelAndConsumeRemainingEvents()
        }

        assertThat(authenticated).isTrue()
    }

    @Test
    fun `failed login surfaces the error`() = runLifecycleTest { lifecycle ->
        val failure = NetworkError.Http(code = 500)
        val component =
            createComponent(
                lifecycle = lifecycle,
                authRepository = FakeAuthRepository(loginResult = failure.left()),
            )

        component.state.test { awaitItem() }
        component.state.value.eventSink(LoginComponent.Event.LoginClicked)
        runCurrent()

        assertThat(component.state.value.error).isEqualTo(failure)
    }

    private fun createComponent(
        authRepository: AuthRepository = FakeAuthRepository(),
        lifecycle: LifecycleRegistry = LifecycleRegistry(),
        onAuthenticated: () -> Unit = {},
    ): LoginComponent =
        DefaultLoginComponent(
            componentContext = testComponentContext(lifecycle = lifecycle),
            onAuthenticated = onAuthenticated,
            authRepository = authRepository,
        )
}
