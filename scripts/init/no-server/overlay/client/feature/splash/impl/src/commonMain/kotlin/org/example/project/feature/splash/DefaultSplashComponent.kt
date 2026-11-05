package org.example.project.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.feature.splash.presentation.SplashComponent

@AssistedInject
class DefaultSplashComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onNavigateToHome: () -> Unit,
) : SplashComponent, MoleculeComponent<SplashComponent.State>(componentContext) {

    @Composable
    override fun produceState(): SplashComponent.State {
        LaunchedEffect(Unit) { onNavigateToHome() }

        return SplashComponent.State.Loading
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SplashComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onNavigateToHome: () -> Unit,
        ): DefaultSplashComponent
    }
}
