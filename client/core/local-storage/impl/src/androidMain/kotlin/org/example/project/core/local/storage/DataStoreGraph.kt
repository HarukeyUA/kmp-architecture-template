package org.example.project.core.local.storage

import android.content.Context
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.example.project.core.local.storage.SettingsLocalDataSourceImpl.Companion.SETTINGS_FILE_NAME
import org.example.project.core.local.storage.di.DataStoreScope
import org.example.project.core.local.storage.model.SettingsLocalModel

@ContributesTo(AppScope::class)
interface AndroidDataStoreStorageGraph {
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @ForScope(DataStoreScope::class)
    fun provideStorage(context: Context): OkioStorage<SettingsLocalModel> =
        OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = dataStoreJsonSerializer(SettingsLocalModel()),
            producePath = { context.filesDir.resolve(SETTINGS_FILE_NAME).toOkioPath() },
        )
}
