package org.example.project.server.feature.tasks.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.example.project.server.auth.AccountId
import org.example.project.server.testing.ExposedTestDb

private val TasksTestDb = ExposedTestDb(Tasks)

class TaskRepositoryTest {
    private val repository = DefaultTaskRepository()
    private val alice = AccountId(UUID.randomUUID())
    private val bob = AccountId(UUID.randomUUID())

    @BeforeTest
    fun setUp() {
        TasksTestDb.connect()
        TasksTestDb.reset()
    }

    @Test
    fun `create returns exactly what later reads see`(): Unit = runBlocking {
        val created = repository.create(alice, "Buy milk")

        assertThat(created.title).isEqualTo("Buy milk")
        assertThat(created.done).isFalse()
        assertThat(created.accountId).isEqualTo(alice)
        assertThat(repository.find(alice, created.id)).isEqualTo(created)
    }

    @Test
    fun `list returns only the account's tasks, newest first`(): Unit = runBlocking {
        val first = repository.create(alice, "First")
        val second = repository.create(alice, "Second")
        repository.create(bob, "Bob's")

        assertThat(repository.listFor(alice)).containsExactly(second, first)
    }

    @Test
    fun `list is empty for an account with no tasks`(): Unit = runBlocking {
        repository.create(alice, "Alice's")

        assertThat(repository.listFor(bob)).isEmpty()
    }

    @Test
    fun `find returns null for a missing, malformed or foreign id`(): Unit = runBlocking {
        val task = repository.create(alice, "Alice's")

        assertThat(repository.find(alice, UUID.randomUUID().toString())).isNull()
        assertThat(repository.find(alice, "not-a-uuid")).isNull()
        assertThat(repository.find(bob, task.id)).isNull()
    }

    @Test
    fun `setDone updates the row and can undo it`(): Unit = runBlocking {
        val task = repository.create(alice, "Alice's")

        val done = repository.setDone(alice, task.id, done = true)
        assertThat(done).isEqualTo(task.copy(done = true))
        assertThat(repository.find(alice, task.id)).isEqualTo(done)

        assertThat(repository.setDone(alice, task.id, done = false)).isEqualTo(task)
    }

    @Test
    fun `setDone leaves another account's task untouched`(): Unit = runBlocking {
        val task = repository.create(alice, "Alice's")

        assertThat(repository.setDone(bob, task.id, done = true)).isNull()
        assertThat(repository.setDone(alice, "not-a-uuid", done = true)).isNull()
        assertThat(repository.find(alice, task.id)).isEqualTo(task)
    }

    @Test
    fun `delete removes only the owner's task`(): Unit = runBlocking {
        val task = repository.create(alice, "Alice's")

        assertThat(repository.delete(bob, task.id)).isFalse()
        assertThat(repository.delete(alice, "not-a-uuid")).isFalse()
        assertThat(repository.find(alice, task.id)).isEqualTo(task)

        assertThat(repository.delete(alice, task.id)).isTrue()
        assertThat(repository.find(alice, task.id)).isNull()
        assertThat(repository.delete(alice, task.id)).isFalse()
    }
}
