package org.example.project.feature.session

import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.example.project.core.local.storage.DataStoreDirectory
import org.example.project.core.local.storage.DefaultDataStoreFactory

/** The repository over a real file: what a corrupt file on disk reads as, end to end. */
@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class SessionFileTest {
    private lateinit var directory: Path

    @BeforeTest
    fun setUp() {
        directory = Files.createTempDirectory("session-file-test")
    }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `no file reads as Unbound`() = runTest {
        assertThat(repository().snapshot()).isEqualTo(SessionState.Unbound.right())
    }

    @Test
    fun `a persisted session lands in its own file and reads back`() = runTest {
        val session = ClientSession("access", "refresh")
        val repository = repository()

        assertThat(repository.persist(session).isRight()).isTrue()

        assertThat(repository.snapshot()).isEqualTo(SessionState.Bound(session).right())
        assertThat(directory.resolve(SESSION_FILE).readText())
            .isEqualTo(
                """{"type":"present","session":{"accessToken":"access","refreshToken":"refresh"}}""",
            )
    }

    @Test
    fun `a corrupt file is a failed read, and clearing it signs out`() = runTest {
        directory.resolve(SESSION_FILE).writeText("not json")
        val repository = repository()

        assertThat(repository.snapshot().leftOrNull())
            .isNotNull()
            .isInstanceOf(SessionRepositoryError.Storage::class)

        val published =
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(SILENCE_WINDOW) { repository.observe().first() }
            }
        assertThat(published).isNull()

        assertThat(repository.clear().isRight()).isTrue()
        assertThat(repository.observe().first()).isEqualTo(SessionState.Unbound)
    }

    @Test
    fun `a corrupt file can be replaced by signing in again`() = runTest {
        directory.resolve(SESSION_FILE).writeText("""{"type":"from-the-future"}""")
        val session = ClientSession("access", "refresh")
        val repository = repository()

        assertThat(repository.persist(session).isRight()).isTrue()

        assertThat(repository.snapshot()).isEqualTo(SessionState.Bound(session).right())
    }

    @Test
    fun `a directory where the file should be is a failed read`() = runTest {
        directory.resolve(SESSION_FILE).createDirectories()

        assertThat(repository().snapshot().leftOrNull())
            .isNotNull()
            .isInstanceOf(SessionRepositoryError.Storage::class)
    }

    private fun repository() =
        DefaultSessionRepository(
            DefaultDataStoreFactory(
                directory = DataStoreDirectory(FileSystem.SYSTEM) { directory.toOkioPath() },
                ioDispatcher = Dispatchers.IO,
            ),
        )

    private companion object {
        const val SESSION_FILE = "session.json"
        val SILENCE_WINDOW = 300.milliseconds
    }
}
