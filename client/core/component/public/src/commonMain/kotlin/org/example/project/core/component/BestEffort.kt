package org.example.project.core.component

import arrow.core.Either
import arrow.core.left
import co.touchlab.kermit.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Runs one step of a sequence that must reach its end: a failure is logged as "could not [what]"
 * and answered as a Left instead of unwinding the steps after it. Cancellation still propagates,
 * since a caller that was told to stop must not carry on.
 */
suspend fun <A> bestEffort(log: Logger, what: String, step: suspend () -> A): Either<Throwable, A> =
    Either.catch { step() }.onLeft { log.w(it) { "could not $what" } }

/**
 * [bestEffort] for a sequence nothing may interrupt, run under [NonCancellable]: a step is answered
 * as failed whatever it throws, a cancellation shape (handed back by a coroutine it waited on,
 * never this caller being cancelled) and a fatal error alike, since a step skipped past would leave
 * what the sequence exists to take away.
 */
// Whatever the step throws, fatal errors included, becomes the answer.
@Suppress("TooGenericExceptionCaught")
suspend fun <A> bestEffortUninterruptible(
    log: Logger,
    what: String,
    step: suspend () -> A,
): Either<Throwable, A> =
    withContext(NonCancellable) {
        try {
            Either.Right(step())
        } catch (e: Throwable) {
            log.w(e) { "could not $what" }
            e.left()
        }
    }
