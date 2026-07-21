package org.example.project.feature.auth

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.shared.auth.InvalidCredentials

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
            createComponent(
                lifecycle = lifecycle,
                authRepository = FakeAuthRepository(loginResult = Unit.right()),
                onAuthenticated = { authenticated = true },
            )

        component.state.test {
            awaitItem().eventSink(LoginComponent.Event.LoginClicked)
            runCurrent()
            cancelAndConsumeRemainingEvents()
        }

        assertThat(authenticated).isTrue()
    }

    @Test
    fun `invalid credentials surface as an inline form error`() = runLifecycleTest { lifecycle ->
        val component =
            createComponent(
                lifecycle = lifecycle,
                authRepository =
                    FakeAuthRepository(
                        loginResult = CallFailure.Declared(InvalidCredentials).left()
                    ),
            )

        component.state.test { awaitItem() }
        component.state.value.eventSink(LoginComponent.Event.LoginClicked)
        runCurrent()

        assertThat(component.state.value.formError)
            .isEqualTo(LoginComponent.FormError.InvalidCredentials)
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
