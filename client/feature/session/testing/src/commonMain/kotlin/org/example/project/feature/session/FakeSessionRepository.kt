package org.example.project.feature.session

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A scriptable session. [nextPersistError] and [nextClearError] each fail one call;
 * [nextSnapshotError] fails every read until it is cleared.
 */
class FakeSessionRepository(initial: SessionState = SessionState.Unbound) : SessionRepository {

    constructor(initial: ClientSession) : this(SessionState.Bound(initial))

    private val state = MutableStateFlow(initial)
    val persisted: MutableList<ClientSession> = mutableListOf()
    var nextSnapshotError: SessionRepositoryError? = null
    var nextPersistError: SessionRepositoryError? = null
    var nextClearError: SessionRepositoryError? = null
    var clearCalls: Int = 0
        private set

    val current: SessionState
        get() = state.value

    fun setBound(session: ClientSession) {
        state.value = SessionState.Bound(session)
    }

    fun setUnbound() {
        state.value = SessionState.Unbound
    }

    override fun observe(): Flow<SessionState> = state

    override suspend fun snapshot(): Either<SessionRepositoryError, SessionState> =
        nextSnapshotError?.let { Either.Left(it) } ?: Either.Right(state.value)

    override suspend fun persist(session: ClientSession): Either<SessionRepositoryError, Unit> {
        nextPersistError?.let {
            nextPersistError = null
            return Either.Left(it)
        }
        persisted += session
        setBound(session)
        return Either.Right(Unit)
    }

    override suspend fun clear(): Either<SessionRepositoryError, Unit> {
        nextClearError?.let {
            nextClearError = null
            return Either.Left(it)
        }
        clearCalls += 1
        setUnbound()
        return Either.Right(Unit)
    }
}
