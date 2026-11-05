package org.example.project.core.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPathApi::class)
class DefaultDatabaseWiperTest {
    private val directory = Files.createTempDirectory("database-wiper-test")
    private val database = openTestDatabase(directory)
    private val wiper = DefaultDatabaseWiper(database)

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `wipe deletes every row and leaves the database usable`() = runTest {
        database.insertRow(1)
        database.insertRow(2)

        wiper.wipe()

        assertThat(database.rowCount()).isEqualTo(0)
        database.insertRow(1)
        assertThat(database.rowCount()).isEqualTo(1)
    }

    @Test
    fun `wipe succeeds on an empty database`() = runTest {
        wiper.wipe()

        assertThat(database.rowCount()).isEqualTo(0)
    }

    // Screens observe Room, so a wipe that stayed silent would leave one account's rows on screen.
    @Test
    fun `wipe notifies observers of the table`() = runTest {
        database.insertRow(1)

        database.invalidationTracker.createFlow(TEST_TABLE).test {
            awaitItem()

            wiper.wipe()

            awaitItem()
            assertThat(database.rowCount()).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
