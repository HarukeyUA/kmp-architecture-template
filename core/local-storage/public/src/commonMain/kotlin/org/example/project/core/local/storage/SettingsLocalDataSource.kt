package org.example.project.core.local.storage

import kotlinx.coroutines.flow.Flow
import org.example.project.core.local.storage.model.SettingsLocalModel

interface SettingsLocalDataSource {
    val settingsFlow: Flow<SettingsLocalModel>
    suspend fun updateSettings(block: suspend (settings: SettingsLocalModel) -> SettingsLocalModel)
    suspend fun reset()
}