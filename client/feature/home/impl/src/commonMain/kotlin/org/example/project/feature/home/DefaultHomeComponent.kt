package org.example.project.feature.home

import androidx.compose.runtime.Composable
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
    @Assisted private val onLogout: () -> Unit,
    private val authRepository: AuthRepository,
) : HomeComponent, MoleculeComponent<HomeComponent.State>(componentContext) {

    @Composable
    override fun produceState(): HomeComponent.State =
        HomeComponent.State(
            eventSink = { event ->
                when (event) {
                    HomeComponent.Event.LogoutClicked -> logout()
                }
            },
        )

    private fun logout() {
        scope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onLogout: () -> Unit,
        ): DefaultHomeComponent
    }
}
