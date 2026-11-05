package org.example.project.feature.session

import arrow.core.Either

/**
 * The whole session contract: the read side, plus the two mutations that sign this device in and
 * out. Injected only where a session is minted or destroyed; readers take [SessionStatus].
 */
interface SessionRepository : SessionStatus {

    /** Makes [session] this device's session, replacing whatever was stored before. */
    suspend fun persist(session: ClientSession): Either<SessionRepositoryError, Unit>

    /** Signs this device out, and also succeeds over a stored session that cannot be read. */
    suspend fun clear(): Either<SessionRepositoryError, Unit>
}
