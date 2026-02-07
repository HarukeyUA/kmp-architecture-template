package org.example.project.feature.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.MoleculeComponent

@AssistedInject
class DefaultHomeListComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted private val onItemSelected: (id: Int) -> Unit,
) :
    HomeListComponent,
    MoleculeComponent<HomeListComponent.State, HomeListComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): HomeListComponent.State {
        CollectEvents { event ->
            when (event) {
                is HomeListComponent.Event.ItemClick -> onItemSelected(event.id)
            }
        }

        return HomeListComponent.State(
            items =
                listOf(
                    HomeListComponent.Item(id = 1, title = "First Item"),
                    HomeListComponent.Item(id = 2, title = "Second Item"),
                    HomeListComponent.Item(id = 3, title = "Third Item"),
                )
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeListComponent.Factory {
        override fun create(
            componentContext: ComponentContext,
            onItemSelected: (id: Int) -> Unit,
        ): DefaultHomeListComponent
    }
}
