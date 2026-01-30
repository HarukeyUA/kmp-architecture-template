package org.example.project.core.local.storage

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.local.storage.model.SettingsLocalModel

@ContributesBinding(AppScope::class)
@Inject
class SettingsLocalDataSourceImpl(private val dataStore: DataStore<SettingsLocalModel>) :
    SettingsLocalDataSource {
    override val settingsFlow = dataStore.data

    override suspend fun updateSettings(
        block: suspend (settings: SettingsLocalModel) -> SettingsLocalModel
    ) {
        dataStore.updateData(block)
    }

    override suspend fun reset() {
        dataStore.updateData { SettingsLocalModel() }
    }

    companion object {
        const val SETTINGS_FILE_NAME = "settings.json"
    }
}
