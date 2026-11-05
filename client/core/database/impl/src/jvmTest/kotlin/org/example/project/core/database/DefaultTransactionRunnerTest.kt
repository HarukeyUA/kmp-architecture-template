package org.example.project.core.database

import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPathApi::class)
class DefaultTransactionRunnerTest {
    private val directory = Files.createTempDirectory("transaction-runner-test")
    private val database = openTestDatabase(directory)
    private val runner = DefaultTransactionRunner(database)

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `a completed block commits every write`() = runTest {
        val result = runner.runInTransaction {
            database.insertRow(1)
            database.insertRow(2)
            "done"
        }

        assertThat(result).isEqualTo("done")
        assertThat(database.rowCount()).isEqualTo(2)
    }

    @Test
    fun `a throwing block rolls back and rethrows`() = runTest {
        assertFailsWith<IllegalStateException> {
            runner.runInTransaction {
                database.insertRow(1)
                error("boom")
            }
        }

        assertThat(database.rowCount()).isEqualTo(0)
    }

    @Test
    fun `a Left rolls back and surfaces`() = runTest {
        val result =
            runner.runTransactionally<String, Int> {
                database.insertRow(1)
                "boom".left()
            }

        assertThat(result).isEqualTo("boom".left())
        assertThat(database.rowCount()).isEqualTo(0)
    }

    @Test
    fun `a Right commits`() = runTest {
        val result =
            runner.runTransactionally<String, Int> {
                database.insertRow(1)
                1.right()
            }

        assertThat(result).isEqualTo(1.right())
        assertThat(database.rowCount()).isEqualTo(1)
    }
}
