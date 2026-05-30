package org.example.project.feature.auth

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.NetworkError
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.shared.common.Unauthorized

class DefaultLoginComponentTest {
    @Test
    fun `email and password edits update state`() = runLifecycleTest { lifecycle ->
        val component = createComponent(lifecycle = lifecycle)

        component.state.test {
            assertThat(awaitItem().email).isEqualTo("")
            component.onEvent(LoginComponent.Event.EmailChanged("alice@example.com"))
            assertThat(awaitItem().email).isEqualTo("alice@example.com")
            component.onEvent(LoginComponent.Event.PasswordChanged("hunter2hunter2"))
            assertThat(awaitItem().password).isEqualTo("hunter2hunter2")
        }
    }

    @Test
    fun `successful login invokes the authenticated callback`() = runLifecycleTest { lifecycle ->
        var authenticated = false
        val component =
            createComponent(
                lifecycle = lifecycle,
                authRepository = FakeAuthRepository(result = Unit.right()),
                onAuthenticated = { authenticated = true },
            )

        component.state.test {
            awaitItem()
            component.onEvent(LoginComponent.Event.LoginClicked)
            runCurrent()
            cancelAndConsumeRemainingEvents()
        }

        assertThat(authenticated).isTrue()
    }

    @Test
    fun `failed login surfaces the error in state`() = runLifecycleTest { lifecycle ->
        val component =
            createComponent(
                lifecycle = lifecycle,
                authRepository = FakeAuthRepository(result = NetworkError.Api(Unauthorized).left()),
            )

        component.state.test { awaitItem() }
        component.onEvent(LoginComponent.Event.LoginClicked)
        runCurrent()

        assertThat(component.state.value.error).isNotNull()
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
