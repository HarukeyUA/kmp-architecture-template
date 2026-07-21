package org.example.project.feature.home.list.presentation

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent

@AssistedInject
class DefaultHomeListComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onItemSelected: (id: Int) -> Unit,
) : HomeListComponent, MoleculeComponent<HomeListComponent.State>(componentContext) {

    @Composable
    override fun produceState(): HomeListComponent.State =
        HomeListComponent.State(
            items =
                listOf(
                    HomeListComponent.Item(id = 1, title = "First Item"),
                    HomeListComponent.Item(id = 2, title = "Second Item"),
                    HomeListComponent.Item(id = 3, title = "Third Item"),
                ),
            eventSink = { event ->
                when (event) {
                    is HomeListComponent.Event.ItemClick -> onItemSelected(event.id)
                }
            },
        )

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeListComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onItemSelected: (id: Int) -> Unit,
        ): DefaultHomeListComponent
    }
}
