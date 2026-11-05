package org.example.project.core.database

import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class RaiseableTransactionTest {

    private val runner = RecordingTransactionRunner()

    @Test
    fun `a Right commits and is returned`() = runTest {
        val result = runner.runTransactionally<String, Int> { 1.right() }

        assertThat(result).isEqualTo(1.right())
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.Committed))
    }

    @Test
    fun `a Left rolls the transaction back and surfaces as the Left`() = runTest {
        val result = runner.runTransactionally<String, Int> { "boom".left() }

        assertThat(result).isEqualTo("boom".left())
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.RolledBack))
    }

    @Test
    fun `the Raise variant raises into the caller's context after rolling back`() = runTest {
        val result = either { runner.runTransactionally<String, Int>(this) { raise("boom") } }

        assertThat(result).isEqualTo("boom".left())
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.RolledBack))
    }

    @Test
    fun `the Raise variant returns the block's value`() = runTest {
        val result = either { runner.runTransactionally<String, Int>(this) { 7 } }

        assertThat(result).isEqualTo(7.right())
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.Committed))
    }

    // The inner call translates its own abort before the outer's block sees anything, so the outer
    // receives the inner's Left as a plain value and its own transaction commits on it.
    @Test
    fun `a nested call's Left is a value to the enclosing call`() = runTest {
        val result =
            runner.runTransactionally<Int, String> {
                val inner = runner.runTransactionally<String, Int> { "inner".left() }
                inner.fold(ifLeft = { it.right() }, ifRight = { "unexpected".right() })
            }

        assertThat(result).isEqualTo("inner".right())
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.RolledBack, Outcome.Committed))
    }

    @Test
    fun `an unrelated throw is not read as a Left`() = runTest {
        assertFailsWith<IllegalStateException> {
            runner.runTransactionally<String, Int> { error("not an abort") }
        }
        assertThat(runner.outcomes).isEqualTo(listOf(Outcome.RolledBack))
    }

    private enum class Outcome {
        Committed,
        RolledBack,
    }

    /** Commits a block that completes and rolls back one that throws, as Room does. */
    private class RecordingTransactionRunner : TransactionRunner {
        val outcomes = mutableListOf<Outcome>()

        override suspend fun <T> runInTransaction(block: suspend () -> T): T {
            var completed = false
            try {
                return block().also { completed = true }
            } finally {
                outcomes += if (completed) Outcome.Committed else Outcome.RolledBack
            }
        }
    }
}
