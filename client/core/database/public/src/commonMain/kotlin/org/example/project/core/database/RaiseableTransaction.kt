package org.example.project.core.database

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either

/**
 * Runs [block] inside a [TransactionRunner] transaction. A [Either.Left] rolls the transaction back
 * and surfaces as the returned Either; without this bridge it would commit while the caller sees a
 * failure.
 */
suspend fun <E : Any, T> TransactionRunner.runTransactionally(
    block: suspend () -> Either<E, T>,
): Either<E, T> =
    try {
        val value = runInTransaction { block().getOrElse { throw TransactionAbortException(it) } }
        Either.Right(value)
    } catch (e: TransactionAbortException) {
        // An exception cannot carry E, but only this frame's block can have thrown this one.
        @Suppress("UNCHECKED_CAST") Either.Left(e.error as E)
    }

/** [runTransactionally] for callers in a [Raise] context: a rollback raises into [raise]. */
suspend fun <E : Any, T> TransactionRunner.runTransactionally(
    raise: Raise<E>,
    block: suspend Raise<E>.() -> T,
): T = with(raise) { runTransactionally<E, T> { either { block() } }.bind() }

/**
 * Marker exception used purely for control flow. Thrown and caught in the same [runTransactionally]
 * frame, so a nested call's abort never reaches the enclosing one.
 */
private class TransactionAbortException(val error: Any) : RuntimeException()
