package org.example.project.core.local.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.example.project.core.local.storage.SettingsLocalDataSourceImpl.Companion.SETTINGS_FILE_NAME
import org.example.project.core.local.storage.model.SettingsLocalModel

@ContributesTo(AppScope::class)
interface DataStoreGraph {
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @SingleIn(AppScope::class)
    fun provideDataStore(): DataStore<SettingsLocalModel> =
        DataStoreFactory.create(
            storage =
                OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = dataStoreJsonSerializer(SettingsLocalModel()),
                    producePath = {
                        // TODO: Store data in a persistent OS dependent dir
                        File(System.getProperty("java.io.tmpdir"), SETTINGS_FILE_NAME).toOkioPath()
                    },
                ),
            corruptionHandler = ReplaceFileCorruptionHandler { SettingsLocalModel() },
        )
}
