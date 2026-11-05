package org.example.project.feature.home

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent

@AssistedInject
class DefaultHomeComponent(@Assisted componentContext: AppComponentContext) :
    HomeComponent, MoleculeComponent<HomeComponent.State>(componentContext) {

    @Composable override fun produceState(): HomeComponent.State = HomeComponent.State

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultHomeComponent
    }
}
