package org.example.project.integration

import androidx.room3.Room
import arrow.core.Either
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.example.project.core.database.AppDatabase
import org.example.project.core.database.AppDatabaseConstructor
import org.example.project.core.database.DefaultDatabaseWiper
import org.example.project.core.database.di.DatabaseGraph
import org.example.project.core.error.CallFailure
import org.example.project.core.network.call
import org.example.project.feature.session.DefaultSessionEndWiper
import org.example.project.feature.session.SessionState
import org.example.project.feature.tasks.TasksRepository
import org.example.project.server.testing.serverTest
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.LogoutRequest
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskTitle

/**
 * The real repository, the real Room schema and the real server together: what a write sends is
 * what the server stores, what the cache keeps is what the server answered, and one account's
 * cached rows never reach the next account on the device.
 */
@OptIn(ExperimentalPathApi::class)
class TasksRepositoryIntegrationTest {
    private val directory: Path = Files.createTempDirectory("tasks-integration")
    private val database: AppDatabase = openDatabase(directory.resolve("device.db"))

    @AfterTest
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    @Test
    fun `create, toggle and delete write through to the cache`() = serverTest {
        val device = clientStack()
        device.auth.signup(ALICE, PASSWORD)
        val tasks = device.tasks(database)

        tasks.create(title("Buy milk")).requireRight()
        val created = tasks.cached().single()
        assertThat(created.title).isEqualTo("Buy milk")
        assertThat(created.done).isEqualTo(false)

        tasks.setDone(created.id, done = true).requireRight()
        assertThat(tasks.observeTask(created.id).first()?.done).isEqualTo(true)

        tasks.delete(created.id).requireRight()
        assertThat(tasks.cached()).isEmpty()
    }

    @Test
    fun `refresh replaces the cache with the server's list, newest first`() = serverTest {
        val laptop = clientStack()
        laptop.auth.signup(ALICE, PASSWORD)
        val elsewhere = Files.createTempDirectory(directory, "laptop").resolve("laptop.db")
        val laptopDatabase = openDatabase(elsewhere)
        try {
            val laptopTasks = laptop.tasks(laptopDatabase)
            laptopTasks.create(title("Older")).requireRight()
            laptopTasks.create(title("Newer")).requireRight()

            val phone = clientStack()
            phone.auth.login(ALICE, PASSWORD).requireRight()
            val phoneTasks = phone.tasks(database)
            assertThat(phoneTasks.cached()).isEmpty()

            phoneTasks.refresh().requireRight()
            assertThat(phoneTasks.cachedTitles()).containsExactly("Newer", "Older")
            // A cached timestamp survives the round trip through the entity.
            assertThat(phoneTasks.cached()).isEqualTo(laptopTasks.cached())

            val older = laptopTasks.cached().last()
            laptopTasks.delete(older.id).requireRight()
            phoneTasks.refresh().requireRight()
            assertThat(phoneTasks.cachedTitles()).containsExactly("Newer")
        } finally {
            laptopDatabase.close()
        }
    }

    @Test
    fun `a task another account owns reads as not found and stays out of the cache`() = serverTest {
        val owner = clientStack()
        owner.auth.signup(ALICE, PASSWORD)
        val ownerDatabase = openDatabase(directory.resolve("owner.db"))
        try {
            val ownerTasks = owner.tasks(ownerDatabase)
            ownerTasks.create(title("Private")).requireRight()
            val id = ownerTasks.cached().single().id

            val other = clientStack()
            other.auth.signup(BOB, PASSWORD)
            val otherTasks = other.tasks(database)

            otherTasks.refresh().requireRight()
            assertThat(otherTasks.cached()).isEmpty()
            assertThat(otherTasks.refresh(id).leftOrNull())
                .isEqualTo(CallFailure.Declared(TaskNotFound))
            assertThat(otherTasks.setDone(id, done = true).leftOrNull())
                .isEqualTo(CallFailure.Declared(TaskNotFound))
            assertThat(otherTasks.observeTask(id).first()).isNull()

            // The owner's task came through all of that untouched.
            ownerTasks.refresh().requireRight()
            assertThat(ownerTasks.cached().single().done).isEqualTo(false)
        } finally {
            ownerDatabase.close()
        }
    }

    @Test
    fun `a task deleted elsewhere drops from the cache on its next read`() = serverTest {
        val device = clientStack()
        device.auth.signup(ALICE, PASSWORD)
        val tasks = device.tasks(database)
        tasks.create(title("Gone soon")).requireRight()
        val id = tasks.cached().single().id

        val secondDevice = clientStack()
        secondDevice.auth.login(ALICE, PASSWORD).requireRight()
        val secondDatabase = openDatabase(directory.resolve("second.db"))
        try {
            val secondTasks = secondDevice.tasks(secondDatabase)
            secondTasks.refresh().requireRight()
            secondTasks.delete(id).requireRight()
        } finally {
            secondDatabase.close()
        }

        assertThat(tasks.refresh(id).leftOrNull()).isEqualTo(CallFailure.Declared(TaskNotFound))
        assertThat(tasks.observeTask(id).first()).isNull()
    }

    @Test
    fun `no write reaches the server or the cache once signed out`() = serverTest {
        val device = clientStack()
        device.auth.signup(ALICE, PASSWORD)
        val tasks = device.tasks(database)
        tasks.create(title("Kept")).requireRight()
        device.auth.logout().requireRight()

        assertThat(tasks.create(title("Rejected")).isLeft()).isEqualTo(true)
        assertThat(tasks.cachedTitles()).containsExactly("Kept")
    }

    @Test
    fun `the next account on a device sees none of the last account's tasks`() = serverTest {
        val device = clientStack()
        val tasks = device.tasks(database)

        withSessionEndWiper(device) {
            device.auth.signup(ALICE, PASSWORD).requireRight()
            tasks.create(title("Alice's secret")).requireRight()
            assertThat(tasks.cachedTitles()).containsExactly("Alice's secret")

            device.auth.logout().requireRight()
            awaitCacheEmpty(tasks)

            device.auth.signup(BOB, PASSWORD).requireRight()
            assertThat(tasks.cached()).isEmpty()
            tasks.refresh().requireRight()
            assertThat(tasks.cached()).isEmpty()

            tasks.create(title("Bob's own")).requireRight()
            assertThat(tasks.cachedTitles()).containsExactly("Bob's own")
        }
    }

    @Test
    fun `a rejected refresh also wipes the cache`() = serverTest {
        val device = clientStack()
        val tasks = device.tasks(database)

        withSessionEndWiper(device) {
            device.auth.signup(ALICE, PASSWORD).requireRight()
            tasks.create(title("Alice's secret")).requireRight()
            val signedIn = (device.sessions.current as SessionState.Bound).session
            // Revoked on the server only, as if from another device, with a dead access token left
            // here to force a refresh, which the server then rejects.
            device.http.call(
                AuthApi.logout,
                AuthResource.Logout(),
                LogoutRequest(signedIn.refreshToken),
            )
            device.sessions.setBound(signedIn.copy(accessToken = "not-a-jwt"))

            assertThat(tasks.refresh().isLeft()).isEqualTo(true)

            assertThat(device.sessions.current).isEqualTo(SessionState.Unbound)

            awaitCacheEmpty(tasks)
        }
    }

    private suspend fun withSessionEndWiper(device: ClientStack, block: suspend () -> Unit) =
        coroutineScope {
            val watcher =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    DefaultSessionEndWiper(device.sessions, DefaultDatabaseWiper(database)).watch()
                }
            try {
                block()
            } finally {
                watcher.cancel()
            }
        }

    private suspend fun awaitCacheEmpty(tasks: TasksRepository) {
        withTimeout(WIPE_TIMEOUT) { tasks.observeTasks().first { it.isEmpty() } }
    }

    private suspend fun TasksRepository.cached() = observeTasks().first()

    private suspend fun TasksRepository.cachedTitles() = cached().map { task -> task.title }

    private fun title(raw: String): TaskTitle = checkNotNull(TaskTitle.of(raw).getOrNull())

    private fun <A : Any, B> Either<A, B>.requireRight(): B =
        fold(ifLeft = { error("expected Right, was $it") }, ifRight = { it })

    private companion object {
        const val ALICE = "alice@example.com"
        const val BOB = "bob@example.com"
        const val PASSWORD = "correct-horse-battery"
        val WIPE_TIMEOUT = 10.seconds

        fun openDatabase(file: Path): AppDatabase =
            DatabaseGraph.provideAppDatabase(
                Room.databaseBuilder(
                    name = file.toString(),
                    factory = AppDatabaseConstructor::initialize,
                ),
                Dispatchers.IO,
            )
    }
}
