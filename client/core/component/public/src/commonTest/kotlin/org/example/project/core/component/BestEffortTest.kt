package org.example.project.core.component

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class BestEffortTest {

    private val log = Logger.withTag("BestEffortTest")

    @Test
    fun `a step that fails answers Left and the sequence carries on`() = runTest {
        val failure = IllegalStateException("no room on disk")
        val ran = mutableListOf<String>()

        bestEffort(log, "first") { throw failure }
        bestEffort(log, "second") { ran += "second" }

        assertThat(ran).isEqualTo(listOf("second"))
    }

    @Test
    fun `a step that succeeds answers its value`() = runTest {
        assertThat(bestEffort(log, "count") { 3 }).isEqualTo(Either.Right(3))
    }

    // A cancelled caller is not a failed step: swallowing it would keep a sequence running on a
    // job that was told to stop, and skip nothing that the caller still expects to have run.
    @Test
    fun `cancelling the caller unwinds through a step`() = runTest {
        var carriedOn = false
        val job = launch {
            bestEffort(log, "wait") { awaitCancellation() }
            carriedOn = true
        }
        runCurrent()

        job.cancel()
        job.join()

        assertThat(job.isCancelled).isTrue()
        assertThat(carriedOn, "the sequence carried on after its job was cancelled").isFalse()
    }

    @Test
    fun `a step throwing in a cancellation shape propagates from a cancellable caller`() = runTest {
        assertFailsWith<CancellationException> {
            bestEffort(log, "wait") { throw CancellationException("handed back") }
        }
    }

    @Test
    fun `the uninterruptible variant answers a cancellation shape as a failed step`() = runTest {
        val result =
            bestEffortUninterruptible(log, "wait") { throw CancellationException("handed back") }

        assertThat(result.leftOrNull()).isNotNull().isInstanceOf<CancellationException>()
    }

    @Test
    fun `the uninterruptible variant runs its step to the end while its caller is cancelled`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var finished = false
            val job = launch {
                bestEffortUninterruptible(log, "drain") {
                    started.complete(Unit)
                    release.await()
                    finished = true
                }
            }
            started.await()

            job.cancel()
            release.complete(Unit)
            job.join()

            assertThat(finished, "the cancellation interrupted the step").isTrue()
        }
}
