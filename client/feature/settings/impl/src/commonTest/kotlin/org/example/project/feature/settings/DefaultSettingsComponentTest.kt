package org.example.project.feature.settings

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.local.storage.SettingsLocalDataSource
import org.example.project.core.local.storage.model.SettingsLocalModel
import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError

class DefaultSettingsComponentTest {
    private val settings = FakeSettings()
    private val auth = FakeAuth()

    @Test
    fun `the stored theme shows`() = runLifecycleTest { lifecycle ->
        settings.model.value = SettingsLocalModel(themeMode = ThemeMode.Dark)
        val component = component(lifecycle)
        runCurrent()

        assertThat(component.state.value.themeMode).isEqualTo(ThemeMode.Dark)
    }

    @Test
    fun `selecting a theme stores it`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()

        component.state.value.eventSink(SettingsComponent.Event.ThemeModeSelected(ThemeMode.Light))
        runCurrent()

        assertThat(settings.model.value.themeMode).isEqualTo(ThemeMode.Light)
        assertThat(component.state.value.themeMode).isEqualTo(ThemeMode.Light)
    }

    @Test
    fun `logout runs once and locks the button while it does`() = runLifecycleTest { lifecycle ->
        val component = component(lifecycle)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate

        component.state.value.eventSink(SettingsComponent.Event.LogoutClicked)
        component.state.value.eventSink(SettingsComponent.Event.LogoutClicked)
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

        component.state.value.eventSink(SettingsComponent.Event.LogoutClicked)
        runCurrent()

        assertThat(component.state.value.logoutFailed).isTrue()
        assertThat(component.state.value.isLoggingOut).isFalse()
    }

    private fun component(lifecycle: LifecycleRegistry): SettingsComponent =
        DefaultSettingsComponent(
                componentContext = testComponentContext(lifecycle),
                settings = settings,
                authRepository = auth,
            )
            .apply { state }

    private class FakeSettings : SettingsLocalDataSource {
        val model = MutableStateFlow(SettingsLocalModel())

        override val settingsFlow: Flow<SettingsLocalModel> = model

        override suspend fun updateSettings(
            block: suspend (settings: SettingsLocalModel) -> SettingsLocalModel,
        ) {
            model.value = block(model.value)
        }

        override suspend fun reset() {
            model.value = SettingsLocalModel()
        }
    }

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
