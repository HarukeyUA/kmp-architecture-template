package org.example.project.core.local.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.example.project.core.local.storage.model.SettingsLocalModel

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SettingsLocalDataSourceImpl(factory: DataStoreFactory) : SettingsLocalDataSource {
    private val dataStore =
        factory.create(
            fileName = SETTINGS_FILE_NAME,
            serializer = SettingsLocalModel.serializer(),
            defaultValue = SettingsLocalModel(),
        )

    override val settingsFlow = dataStore.data

    override suspend fun updateSettings(
        block: suspend (settings: SettingsLocalModel) -> SettingsLocalModel,
    ) {
        dataStore.updateData(block)
    }

    override suspend fun reset() {
        dataStore.updateData { SettingsLocalModel() }
    }

    private companion object {
        const val SETTINGS_FILE_NAME = "settings.json"
    }
}
