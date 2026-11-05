package org.example.project.feature.splash

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlinx.coroutines.test.runCurrent
import org.example.project.core.testing.asBranch
import org.example.project.core.testing.runLifecycleTest
import org.example.project.core.testing.testComponentContext
import org.example.project.feature.session.ClientSession
import org.example.project.feature.session.FakeSessionRepository
import org.example.project.feature.session.SessionRepositoryError
import org.example.project.feature.session.SessionState
import org.example.project.feature.splash.presentation.SplashComponent

class DefaultSplashComponentTest {
    private val routes = mutableListOf<String>()
    private val readFailure = SessionRepositoryError.Storage(IllegalStateException("disk gone"))

    @Test
    fun `a stored session routes to home`() = runLifecycleTest { lifecycle ->
        val sessions = FakeSessionRepository(ClientSession("access", "refresh"))

        createComponent(lifecycle, sessions).state
        runCurrent()

        assertThat(routes).isEqualTo(listOf("home"))
    }

    @Test
    fun `no stored session routes to login`() = runLifecycleTest { lifecycle ->
        createComponent(lifecycle, FakeSessionRepository()).state
        runCurrent()

        assertThat(routes).isEqualTo(listOf("login"))
    }

    @Test
    fun `a failed read holds on the splash and clears nothing`() = runLifecycleTest { lifecycle ->
        val sessions = FakeSessionRepository(ClientSession("access", "refresh"))
        sessions.nextSnapshotError = readFailure

        val component = createComponent(lifecycle, sessions)
        component.state
        runCurrent()

        assertThat(component.state.value)
            .isInstanceOf(SplashComponent.State.SessionUnreadable::class)
        assertThat(routes).isEqualTo(emptyList())
        assertThat(sessions.clearCalls).isEqualTo(0)
    }

    @Test
    fun `retry routes once the read recovers`() = runLifecycleTest { lifecycle ->
        val sessions = FakeSessionRepository(ClientSession("access", "refresh"))
        sessions.nextSnapshotError = readFailure
        val component = createComponent(lifecycle, sessions)
        component.state
        runCurrent()

        sessions.nextSnapshotError = null
        component.state.value
            .asBranch<SplashComponent.State.SessionUnreadable>()
            .eventSink(SplashComponent.Event.RetryClicked)
        runCurrent()

        assertThat(routes).isEqualTo(listOf("home"))
    }

    @Test
    fun `retry holds again while the read keeps failing`() = runLifecycleTest { lifecycle ->
        val sessions = FakeSessionRepository()
        sessions.nextSnapshotError = readFailure
        val component = createComponent(lifecycle, sessions)
        component.state
        runCurrent()

        component.state.value
            .asBranch<SplashComponent.State.SessionUnreadable>()
            .eventSink(SplashComponent.Event.RetryClicked)
        runCurrent()

        assertThat(component.state.value)
            .isInstanceOf(SplashComponent.State.SessionUnreadable::class)
        assertThat(routes).isEqualTo(emptyList())
    }

    @Test
    fun `signing in again clears the session and routes to login`() =
        runLifecycleTest { lifecycle ->
            val sessions = FakeSessionRepository(ClientSession("access", "refresh"))
            sessions.nextSnapshotError = readFailure
            val component = createComponent(lifecycle, sessions)
            component.state
            runCurrent()

            component.state.value
                .asBranch<SplashComponent.State.SessionUnreadable>()
                .eventSink(SplashComponent.Event.SignInAgainClicked)
            runCurrent()

            assertThat(sessions.current).isEqualTo(SessionState.Unbound)
            assertThat(routes).isEqualTo(listOf("login"))
        }

    @Test
    fun `a clear that fails stays on the splash`() = runLifecycleTest { lifecycle ->
        val sessions = FakeSessionRepository(ClientSession("access", "refresh"))
        sessions.nextSnapshotError = readFailure
        sessions.nextClearError = readFailure
        val component = createComponent(lifecycle, sessions)
        component.state
        runCurrent()

        component.state.value
            .asBranch<SplashComponent.State.SessionUnreadable>()
            .eventSink(SplashComponent.Event.SignInAgainClicked)
        runCurrent()

        assertThat(routes).isEqualTo(emptyList())
    }

    private fun createComponent(
        lifecycle: LifecycleRegistry,
        sessions: FakeSessionRepository,
    ): SplashComponent =
        DefaultSplashComponent(
            componentContext = testComponentContext(lifecycle = lifecycle),
            onNavigateToHome = { routes += "home" },
            onNavigateToLogin = { routes += "login" },
            sessions = sessions,
        )
}
