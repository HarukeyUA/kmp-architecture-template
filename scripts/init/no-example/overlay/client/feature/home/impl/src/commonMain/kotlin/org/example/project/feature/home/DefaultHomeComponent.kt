package org.example.project.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.feature.auth.AuthRepository

@AssistedInject
class DefaultHomeComponent(
    @Assisted componentContext: AppComponentContext,
    private val authRepository: AuthRepository,
) : HomeComponent, MoleculeComponent<HomeComponent.State>(componentContext) {

    @Composable
    override fun produceState(): HomeComponent.State {
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

        return HomeComponent.State(
            isLoggingOut = isLoggingOut,
            logoutFailed = logoutFailed,
            eventSink = { event ->
                when (event) {
                    HomeComponent.Event.LogoutClicked -> logout()
                }
            },
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultHomeComponent
    }
}
