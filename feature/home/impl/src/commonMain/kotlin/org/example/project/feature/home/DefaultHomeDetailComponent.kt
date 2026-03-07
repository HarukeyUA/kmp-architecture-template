package org.example.project.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.component.snackbar.SnackbarMessage

@AssistedInject
class DefaultHomeDetailComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val itemId: Int,
    @Assisted private val onBack: () -> Unit,
) :
    HomeDetailComponent,
    MoleculeComponent<HomeDetailComponent.State, HomeDetailComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): HomeDetailComponent.State {
        CollectEvents { event ->
            when (event) {
                HomeDetailComponent.Event.BackClick -> onBack()
            }
        }

        LaunchedEffect(Unit) {
            this@DefaultHomeDetailComponent.snackbarHandler.showSnackbar(
                SnackbarMessage(text = "Message")
            )
        }

        return HomeDetailComponent.State(
            id = itemId,
            title = "Item $itemId",
            description = "This is the detailed description for item $itemId.",
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeDetailComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            itemId: Int,
            onBack: () -> Unit,
        ): DefaultHomeDetailComponent
    }
}
