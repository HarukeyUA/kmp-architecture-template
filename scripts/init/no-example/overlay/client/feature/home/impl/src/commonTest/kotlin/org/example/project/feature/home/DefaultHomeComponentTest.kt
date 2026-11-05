package org.example.project.feature.home

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError

class DefaultHomeComponentTest {
    private val auth = FakeAuth()

    @Test
    fun `logout runs once and locks the button while it does`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate

        component.state.value.eventSink(HomeComponent.Event.LogoutClicked)
        component.state.value.eventSink(HomeComponent.Event.LogoutClicked)
        runCurrent()
        assertThat(component.state.value.isLoggingOut).isTrue()

        gate.complete(Unit)
        runCurrent()

        assertThat(auth.logoutCalls).isEqualTo(1)
        assertThat(component.state.value.isLoggingOut).isFalse()
        assertThat(component.state.value.logoutFailed).isFalse()
    }

    @Test
    fun `a logout that could not sign out says so`() = runLifecycleTest { lifecycle ->
        auth.result =
            CallFailure.Transport(NetworkError.Serialization(IllegalStateException("disk gone")))
                .left()
        val component = component(lifecycle)
        runCurrent()

        component.state.value.eventSink(HomeComponent.Event.LogoutClicked)
        runCurrent()

        assertThat(component.state.value.logoutFailed).isTrue()
        assertThat(component.state.value.isLoggingOut).isFalse()
    }

    private fun component(lifecycle: LifecycleRegistry): HomeComponent =
        DefaultHomeComponent(
                componentContext = testComponentContext(lifecycle),
                authRepository = auth,
            )
            .apply { state }

    private class FakeAuth : AuthRepository {
        var result: Either<CallFailure<Nothing>, Unit> = Unit.right()
        var gate: CompletableDeferred<Unit>? = null
        var logoutCalls = 0
            private set

        override suspend fun login(
            email: String,
            password: String,
        ): Either<CallFailure<AuthLoginError>, Unit> = Unit.right()

        override suspend fun signup(
            email: String,
            password: String,
        ): Either<CallFailure<AuthSignupError>, Unit> = Unit.right()

        override suspend fun logout(): Either<CallFailure<Nothing>, Unit> {
            logoutCalls += 1
            gate?.await()
            return result
        }
    }
}
