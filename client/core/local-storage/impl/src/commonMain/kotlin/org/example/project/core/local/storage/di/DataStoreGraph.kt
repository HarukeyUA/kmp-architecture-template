package org.example.project.core.local.storage.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.example.project.core.local.storage.model.SettingsLocalModel

@ContributesTo(AppScope::class)
interface DataStoreGraph {
    @Provides
    @SingleIn(AppScope::class)
    @ForScope(DataStoreScope::class)
    fun provideDataStore(
        @ForScope(DataStoreScope::class) storage: OkioStorage<SettingsLocalModel>
    ): DataStore<SettingsLocalModel> =
        DataStore.Builder(storage = storage, context = Dispatchers.IO + SupervisorJob())
            .setCorruptionHandler(ReplaceFileCorruptionHandler { SettingsLocalModel() })
            .build()
}
