package org.example.project.core.local.storage.model

import kotlinx.serialization.Serializable

@Serializable data class SettingsLocalModel(val isLoggedIn: Boolean = false)
