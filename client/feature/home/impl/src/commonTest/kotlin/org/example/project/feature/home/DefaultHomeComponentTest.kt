package org.example.project.feature.home

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError

class DefaultHomeComponentTest {
    private val calls = mutableListOf<String>()

    @Test
    fun `logout signs out through the repository before leaving`() = runLifecycleTest { lifecycle ->
        val component = createComponent(lifecycle)

        component.state.test {
            awaitItem().eventSink(HomeComponent.Event.LogoutClicked)
            runCurrent()
            cancelAndConsumeRemainingEvents()
        }

        assertThat(calls).isEqualTo(listOf("repository logout", "left home"))
    }

    private fun createComponent(lifecycle: LifecycleRegistry): HomeComponent =
        DefaultHomeComponent(
            componentContext = testComponentContext(lifecycle = lifecycle),
            onLogout = { calls += "left home" },
            authRepository = RecordingAuthRepository(),
        )

    private inner class RecordingAuthRepository : AuthRepository {
        override suspend fun login(
            email: String,
            password: String,
        ): Either<CallFailure<AuthLoginError>, Unit> = Unit.right()

        override suspend fun signup(
            email: String,
            password: String,
        ): Either<CallFailure<AuthSignupError>, Unit> = Unit.right()

        override suspend fun logout() {
            calls += "repository logout"
        }
    }
}
