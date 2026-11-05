package org.example.project.feature.session

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class DefaultSessionRepositoryTest {
    private val session = ClientSession(accessToken = "access", refreshToken = "refresh")

    @Test
    fun `an absent file reads as Unbound`() = runTest {
        val repository = DefaultSessionRepository(FakeStore(StoredSession.Absent))

        assertThat(repository.snapshot()).isEqualTo(SessionState.Unbound.right())
    }

    @Test
    fun `persist then clear moves the state through Bound and Unbound`() = runTest {
        val repository = DefaultSessionRepository(FakeStore(StoredSession.Absent))

        repository.observe().test {
            assertThat(awaitItem()).isEqualTo(SessionState.Unbound)

            assertThat(repository.persist(session).isRight()).isTrue()
            assertThat(awaitItem()).isEqualTo(SessionState.Bound(session))
            assertThat(repository.snapshot()).isEqualTo(SessionState.Bound(session).right())

            assertThat(repository.clear().isRight()).isTrue()
            assertThat(awaitItem()).isEqualTo(SessionState.Unbound)
        }
    }

    @Test
    fun `an unreadable file fails the snapshot instead of reading as Unbound`() = runTest {
        val repository = DefaultSessionRepository(FakeStore(StoredSession.Unreadable))

        assertThat(repository.snapshot().leftOrNull())
            .isNotNull()
            .isInstanceOf(SessionRepositoryError.Storage::class)
    }

    @Test
    fun `an unreadable file publishes nothing until a write replaces it`() = runTest {
        val repository = DefaultSessionRepository(FakeStore(StoredSession.Unreadable))

        repository.observe().test {
            expectNoEvents()

            assertThat(repository.clear().isRight()).isTrue()
            assertThat(awaitItem()).isEqualTo(SessionState.Unbound)
        }
    }

    @Test
    fun `a failed read fails the snapshot`() = runTest {
        val repository = DefaultSessionRepository(FailingThenWorkingStore(failures = 1))

        assertThat(repository.snapshot().leftOrNull())
            .isNotNull()
            .isInstanceOf(SessionRepositoryError.Storage::class)
    }

    @Test
    fun `observe stays silent through a failed read and recovers`() = runTest {
        val repository = DefaultSessionRepository(FailingThenWorkingStore(failures = 1))

        repository.observe().test {
            assertThat(awaitItem()).isEqualTo(SessionState.Bound(session))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed write is typed`() = runTest {
        val repository = DefaultSessionRepository(FailingThenWorkingStore(failures = 0))

        assertThat(repository.persist(session).leftOrNull())
            .isNotNull()
            .isInstanceOf(SessionRepositoryError.Storage::class)
    }

    @Test
    fun `the persisted shape is pinned`() {
        val encoded =
            Json.encodeToString(StoredSession.serializer(), StoredSession.Present(session))

        assertThat(encoded)
            .isEqualTo(
                """{"type":"present","session":{"accessToken":"access","refreshToken":"refresh"}}""",
            )
    }

    private class FakeStore(initial: StoredSession) : DataStore<StoredSession> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<StoredSession> = state

        override suspend fun updateData(
            transform: suspend (t: StoredSession) -> StoredSession,
        ): StoredSession = transform(state.value).also { state.value = it }
    }

    private inner class FailingThenWorkingStore(private var failures: Int) :
        DataStore<StoredSession> {
        override val data: Flow<StoredSession> = flow {
            if (failures > 0) {
                failures -= 1
                throw READ_FAILURE
            }
            emit(StoredSession.Present(session))
        }

        override suspend fun updateData(
            transform: suspend (t: StoredSession) -> StoredSession,
        ): StoredSession = throw READ_FAILURE
    }

    private companion object {
        val READ_FAILURE = IllegalStateException("disk gone")
    }
}
