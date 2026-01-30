package org.example.project.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.launch
import org.example.project.core.component.MoleculeComponent
import org.example.project.feature.user.data.UserRepository

@AssistedInject
class DefaultProfileComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val onLogout: () -> Unit,
    private val userRepository: UserRepository
) : ProfileComponent, MoleculeComponent<ProfileComponent.State, ProfileComponent.Event>(componentContext) {

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

        return ProfileComponent.State(
            userName = userName,
            email = email
        )
    }

    private fun logout() {
        scope.launch {
            userRepository.logout()
            onLogout()
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : ProfileComponent.Factory {
        override fun create(
            componentContext: ComponentContext,
            onLogout: () -> Unit
        ): DefaultProfileComponent
    }
}
