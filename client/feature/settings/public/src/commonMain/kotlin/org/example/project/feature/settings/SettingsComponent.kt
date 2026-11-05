package org.example.project.feature.settings

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiEvent
import org.example.project.core.component.UiState
import org.example.project.core.local.storage.model.ThemeMode

interface SettingsComponent : StatefulComponent<SettingsComponent.State> {

    data class State(
        val themeMode: ThemeMode = ThemeMode.System,
        val isLoggingOut: Boolean = false,
        /** The last logout did not go through, so the device is still signed in. */
        val logoutFailed: Boolean = false,
        val eventSink: (Event) -> Unit,
    ) : UiState

    sealed interface Event : UiEvent {
        data class ThemeModeSelected(val mode: ThemeMode) : Event

        data object LogoutClicked : Event
    }

    fun interface Factory {
        fun create(componentContext: AppComponentContext): SettingsComponent
    }
}
