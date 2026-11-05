package org.example.project.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.local.storage.SettingsLocalDataSource
import org.example.project.core.local.storage.model.SettingsLocalModel
import org.example.project.core.local.storage.model.ThemeMode
import org.example.project.feature.auth.AuthRepository
import org.example.project.feature.settings.SettingsComponent.Event
import org.example.project.feature.settings.SettingsComponent.State

@AssistedInject
class DefaultSettingsComponent(
    @Assisted componentContext: AppComponentContext,
    private val settings: SettingsLocalDataSource,
    private val authRepository: AuthRepository,
) : SettingsComponent, MoleculeComponent<State>(componentContext) {

    @Composable
    override fun produceState(): State {
        val current by remember {
            settings.settingsFlow
        }
            .collectAsStateWithLifecycle(initialValue = SettingsLocalModel())
        var isLoggingOut by remember { mutableStateOf(false) }
        var logoutFailed by remember { mutableStateOf(false) }

        fun logout() {
            if (isLoggingOut) return
            Snapshot.withMutableSnapshot {
                isLoggingOut = true
                logoutFailed = false
            }
            // Success needs no navigation here: the root follows the session to the login screen.
            scope.launch {
                val failed = authRepository.logout().isLeft()
                Snapshot.withMutableSnapshot {
                    isLoggingOut = false
                    logoutFailed = failed
                }
            }
        }

        return State(
            themeMode = current.themeMode,
            isLoggingOut = isLoggingOut,
            logoutFailed = logoutFailed,
            eventSink = { event ->
                when (event) {
                    is Event.ThemeModeSelected -> selectTheme(event.mode)
                    Event.LogoutClicked -> logout()
                }
            },
        )
    }

    private fun selectTheme(mode: ThemeMode) {
        scope.launch { settings.updateSettings { it.copy(themeMode = mode) } }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SettingsComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultSettingsComponent
    }
}
