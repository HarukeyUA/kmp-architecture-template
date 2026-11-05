package org.example.project.feature.session

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

/**
 * The read side of the session contract, taken by everything that only reads a session. Signing in
 * and out need [SessionRepository].
 */
interface SessionStatus {

    /**
     * The current state, re-emitted whenever it changes. A state that cannot be read emits nothing,
     * since it is not an absent session; [snapshot] is the read that reports such a failure.
     */
    fun observe(): Flow<SessionState>

    /** One-shot read of the current state. */
    suspend fun snapshot(): Either<SessionRepositoryError, SessionState>
}
