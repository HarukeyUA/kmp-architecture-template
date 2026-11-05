package org.example.project.core.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.example.project.core.database.entity.TaskEntity

@OptIn(ExperimentalPathApi::class)
class TaskDaoTest {
    private val directory = Files.createTempDirectory("task-dao-test")
    private val database = openTestDatabase(directory)
    private val dao = database.taskDao()

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `tasks read newest first with the id as tie-break`() = runTest {
        dao.upsertAll(listOf(task("a", createdAt = 1), task("b", createdAt = 2), task("c", 2)))

        assertThat(dao.observeAll().first().map { it.id }).containsExactly("c", "b", "a")
    }

    @Test
    fun `upsert replaces the row with the same id`() = runTest {
        dao.upsert(task("a", createdAt = 1))

        dao.upsert(task("a", createdAt = 1).copy(done = true))

        assertThat(dao.observeById("a").first()?.done).isEqualTo(true)
    }

    @Test
    fun `an observed row follows its writes and reads null once deleted`() = runTest {
        dao.upsert(task("a", createdAt = 1))

        dao.observeById("a").test {
            assertThat(awaitItem()?.done).isEqualTo(false)

            dao.upsert(task("a", createdAt = 1).copy(done = true))
            assertThat(awaitItem()?.done).isEqualTo(true)

            dao.deleteById("a")
            assertThat(awaitItem()).isNull()
        }
    }

    private fun task(id: String, createdAt: Long) =
        TaskEntity(id = id, title = id, done = false, createdAt = createdAt)
}
