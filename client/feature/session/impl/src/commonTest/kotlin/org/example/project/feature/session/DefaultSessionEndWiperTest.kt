package org.example.project.feature.session

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.example.project.core.database.DatabaseWiper
import org.example.project.core.database.testing.FakeDatabaseWiper

class DefaultSessionEndWiperTest {
    private val wiper = FakeDatabaseWiper()

    @Test
    fun `a launch with a session wipes nothing`() = runTest {
        val sessions = FakeSessionRepository(SESSION_A)

        watching(sessions, wiper) {}

        assertThat(wiper.wipeCalls).isEqualTo(0)
    }

    // A process killed between the sign-out and the wipe leaves rows with no owner.
    @Test
    fun `a launch with no session wipes`() = runTest {
        watching(FakeSessionRepository(), wiper) {}

        assertThat(wiper.wipeCalls).isEqualTo(1)
    }

    @Test
    fun `the end of a session wipes`() = runTest {
        val sessions = FakeSessionRepository(SESSION_A)

        watching(sessions, wiper) { sessions.clear() }

        assertThat(wiper.wipeCalls).isEqualTo(1)
    }

    @Test
    fun `a minted access token is the same sign-in and wipes nothing`() = runTest {
        val sessions = FakeSessionRepository(SESSION_A)

        watching(sessions, wiper) { sessions.persist(SESSION_A.copy(accessToken = "minted")) }

        assertThat(wiper.wipeCalls).isEqualTo(0)
    }

    @Test
    fun `signing in from signed out wipes only for the launch`() = runTest {
        val sessions = FakeSessionRepository()

        watching(sessions, wiper) { sessions.persist(SESSION_A) }

        assertThat(wiper.wipeCalls).isEqualTo(1)
    }

    @Test
    fun `a sign-in replacing another wipes`() = runTest {
        val sessions = FakeSessionRepository(SESSION_A)

        watching(sessions, wiper) { sessions.persist(SESSION_B) }

        assertThat(wiper.wipeCalls).isEqualTo(1)
    }

    @Test
    fun `a failed wipe is retried until it succeeds`() = runTest {
        val sessions = FakeSessionRepository()
        val failing = FailingOnceWiper()

        val watcher =
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                DefaultSessionEndWiper(sessions, failing).watch()
            }
        runCurrent()
        assertThat(failing.attempts).isEqualTo(1)

        advanceTimeBy(RETRY_MILLIS)
        runCurrent()

        assertThat(failing.attempts).isEqualTo(2)
        watcher.cancel()
    }

    private suspend fun TestScope.watching(
        sessions: SessionStatus,
        wiper: DatabaseWiper,
        act: suspend () -> Unit,
    ) {
        val watcher =
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                DefaultSessionEndWiper(sessions, wiper).watch()
            }
        runCurrent()
        act()
        runCurrent()
        watcher.cancel()
    }

    private class FailingOnceWiper : DatabaseWiper {
        var attempts = 0
            private set

        override suspend fun wipe() {
            attempts++
            check(attempts > 1) { "disk gone" }
        }
    }

    private companion object {
        const val RETRY_MILLIS = 5_000L
        val SESSION_A = ClientSession("access-a", "refresh-a")
        val SESSION_B = ClientSession("access-b", "refresh-b")
    }
}
