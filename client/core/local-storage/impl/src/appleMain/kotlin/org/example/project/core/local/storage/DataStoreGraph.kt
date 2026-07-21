package org.example.project.core.local.storage

import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileSystem
import okio.Path.Companion.toPath
import org.example.project.core.local.storage.SettingsLocalDataSourceImpl.Companion.SETTINGS_FILE_NAME
import org.example.project.core.local.storage.di.DataStoreScope
import org.example.project.core.local.storage.model.SettingsLocalModel
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@ContributesTo(AppScope::class)
interface AppleDataStoreStorageGraph {
    @OptIn(ExperimentalForeignApi::class, ExperimentalSerializationApi::class)
    @Provides
    @ForScope(DataStoreScope::class)
    fun provideStorage(): OkioStorage<SettingsLocalModel> =
        OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = dataStoreJsonSerializer(SettingsLocalModel()),
            producePath = {
                val documentDirectory: NSURL? =
                    NSFileManager.defaultManager.URLForDirectory(
                        directory = NSDocumentDirectory,
                        inDomain = NSUserDomainMask,
                        appropriateForURL = null,
                        create = false,
                        error = null,
                    )
                requireNotNull(documentDirectory?.path).toPath().resolve(SETTINGS_FILE_NAME)
            },
        )
}
