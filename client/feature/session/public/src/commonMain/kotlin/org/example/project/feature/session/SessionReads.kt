package org.example.project.feature.session

import arrow.core.getOrElse
import co.touchlab.kermit.Logger

/**
 * [SessionStatus.snapshot] for a caller that carries on without the state: a failed read is logged
 * to [log] and answers null. Null is not Unbound; nothing may tear down account state on it.
 */
suspend fun SessionStatus.stateOrNull(log: Logger): SessionState? =
    snapshot().getOrElse { error ->
        val cause =
            when (error) {
                is SessionRepositoryError.Storage -> error.cause
            }
        log.w(cause) { "session read failed; treating the session as unreadable" }
        null
    }

/** The session whose tokens are readable now, or null. See [stateOrNull] for a failed read. */
suspend fun SessionStatus.boundSessionOrNull(log: Logger): ClientSession? =
    (stateOrNull(log) as? SessionState.Bound)?.session
