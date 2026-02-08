package org.example.project.feature.auth

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import org.example.project.core.testing.CoroutineTest
import org.example.project.core.testing.runLifecycleTest
import org.example.project.feature.user.data.FakeUserRepository
import org.example.project.feature.user.data.UserRepository

class DefaultLoginComponentTest : CoroutineTest() {
    @Test
    fun `initial state has counter equal to zero`() = runLifecycleTest { lifecycle ->
        val component = createComponent(lifecycle = lifecycle)

        component.state.test { assertThat(awaitItem().counter).isEqualTo(0) }
    }

    @Test
    fun `login click calls user repository and triggers success callback`() =
        runLifecycleTest { lifecycle ->
            val userRepository = FakeUserRepository()
            var loginSuccess = false

            val component =
                createComponent(
                    lifecycle = lifecycle,
                    userRepository = userRepository,
                    onLoginSuccess = { loginSuccess = true },
                )

            component.state.test {
                component.onEvent(LoginComponent.Event.LoginClicked)
                cancelAndConsumeRemainingEvents()
            }

            assertThat(loginSuccess).isTrue()

            userRepository.isLoggedIn.test { assertThat(awaitItem()).isTrue() }
        }

    private fun createComponent(
        userRepository: UserRepository = FakeUserRepository(),
        lifecycle: LifecycleRegistry = LifecycleRegistry(),
        onLoginSuccess: () -> Unit = {},
    ): LoginComponent {
        val context = DefaultComponentContext(lifecycle = lifecycle)

        return DefaultLoginComponent(
            componentContext = context,
            onLoginSuccess = onLoginSuccess,
            userRepository = userRepository,
        )
    }
}
