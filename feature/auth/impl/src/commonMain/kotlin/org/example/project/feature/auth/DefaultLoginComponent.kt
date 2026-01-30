package org.example.project.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.core.component.MoleculeComponent
import org.example.project.feature.user.data.UserRepository
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class DefaultLoginComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val onLoginSuccess: () -> Unit,
    private val userRepository: UserRepository
) : LoginComponent,
    MoleculeComponent<LoginComponent.State, LoginComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): LoginComponent.State {
        var counter by rememberSaveable { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1.seconds)
                counter++
            }
        }

        CollectEvents { event ->
            when (event) {
                LoginComponent.Event.LoginClicked -> {
                    logIn()
                }
            }
        }

        return LoginComponent.State(counter = counter)
    }

    private fun logIn() {
        scope.launch {
            userRepository.setIsLoggedIn(true)
            onLoginSuccess()
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : LoginComponent.Factory {
        override fun create(
            componentContext: ComponentContext,
            onLoginSuccess: () -> Unit
        ): DefaultLoginComponent
    }
}
