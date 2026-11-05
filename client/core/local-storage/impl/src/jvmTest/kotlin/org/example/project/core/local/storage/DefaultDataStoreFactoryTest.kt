package org.example.project.core.local.storage

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.example.project.core.local.storage.model.SettingsLocalModel
import org.example.project.core.local.storage.model.ThemeMode

@OptIn(ExperimentalPathApi::class)
class DefaultDataStoreFactoryTest {
    private val directory = Files.createTempDirectory("data-store-factory-test")
    private val factory =
        DefaultDataStoreFactory(
            directory = DataStoreDirectory(FileSystem.SYSTEM) { directory.toOkioPath() },
            ioDispatcher = Dispatchers.IO,
        )

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `stores keep separate files in the directory`() = runTest {
        factory.create("one.json", Int.serializer(), 0).updateData { 1 }
        factory.create("two.json", Int.serializer(), 0).updateData { 2 }

        assertThat(directory.listDirectoryEntries().map { it.name })
            .containsExactlyInAnyOrder("one.json", "two.json")
    }

    @Test
    fun `an absent file reads as the default value`() = runTest {
        val store = factory.create("absent.json", Int.serializer(), 7, unreadableValue = -1)

        assertThat(store.data.first()).isEqualTo(7)
    }

    @Test
    fun `a file that does not decode reads as the unreadable value`() = runTest {
        directory.resolve("bad.json").writeText("not json")
        val store = factory.create("bad.json", Int.serializer(), 7, unreadableValue = -1)

        assertThat(store.data.first()).isEqualTo(-1)
    }

    @Test
    fun `settings fall back to their default over an unreadable file`() = runTest {
        directory.resolve("settings.json").writeText("not json")

        val settings = SettingsLocalDataSourceImpl(factory)

        assertThat(settings.settingsFlow.first()).isEqualTo(SettingsLocalModel())
    }

    @Test
    fun `settings round trip through their own file`() = runTest {
        val settings = SettingsLocalDataSourceImpl(factory)

        settings.updateSettings { it.copy(themeMode = ThemeMode.Light) }

        assertThat(settings.settingsFlow.first().themeMode).isEqualTo(ThemeMode.Light)
        assertThat(directory.listDirectoryEntries().map { it.name })
            .containsExactlyInAnyOrder("settings.json")
    }
}
