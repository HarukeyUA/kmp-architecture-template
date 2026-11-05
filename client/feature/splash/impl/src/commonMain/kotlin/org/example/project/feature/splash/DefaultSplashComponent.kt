package org.example.project.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.feature.session.SessionRepository
import org.example.project.feature.session.SessionState
import org.example.project.feature.session.stateOrNull
import org.example.project.feature.splash.presentation.SplashComponent

@AssistedInject
class DefaultSplashComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onNavigateToMain: () -> Unit,
    @Assisted private val onNavigateToLogin: () -> Unit,
    private val sessions: SessionRepository,
) : SplashComponent, MoleculeComponent<SplashComponent.State>(componentContext) {
    private val log = Logger.withTag("Splash")

    @Composable
    override fun produceState(): SplashComponent.State {
        var attempt by remember { mutableIntStateOf(0) }
        var unreadable by remember { mutableStateOf(false) }

        LaunchedEffect(attempt) {
            when (sessions.stateOrNull(log)) {
                is SessionState.Bound -> onNavigateToMain()
                SessionState.Unbound -> onNavigateToLogin()
                // Login here would read as signed out while a session may still be on disk.
                null -> unreadable = true
            }
        }

        return if (unreadable) {
            SplashComponent.State.SessionUnreadable { event ->
                when (event) {
                    SplashComponent.Event.RetryClicked ->
                        Snapshot.withMutableSnapshot {
                            unreadable = false
                            attempt += 1
                        }
                    SplashComponent.Event.SignInAgainClicked -> signInAgain()
                }
            }
        } else {
            SplashComponent.State.Loading
        }
    }

    private fun signInAgain() {
        scope.launch {
            sessions
                .clear()
                .fold(
                    ifLeft = { log.e { "session not cleared: $it" } },
                    ifRight = { onNavigateToLogin() },
                )
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SplashComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onNavigateToMain: () -> Unit,
            onNavigateToLogin: () -> Unit,
        ): DefaultSplashComponent
    }
}
