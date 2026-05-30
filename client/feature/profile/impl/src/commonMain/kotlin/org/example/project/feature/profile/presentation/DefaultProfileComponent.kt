package org.example.project.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
class DefaultProfileComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onLogout: () -> Unit,
    private val authRepository: AuthRepository,
) :
    ProfileComponent,
    MoleculeComponent<ProfileComponent.State, ProfileComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): ProfileComponent.State {
        val userName by rememberSaveable { mutableStateOf("User") }
        val email by rememberSaveable { mutableStateOf("user@example.com") }

        CollectEvents { event ->
            when (event) {
                ProfileComponent.Event.LogoutClicked -> {
                    logout()
                }
            }
        }

        return ProfileComponent.State(userName = userName, email = email)
    }

    private fun logout() {
        scope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : ProfileComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onLogout: () -> Unit,
        ): DefaultProfileComponent
    }
}
