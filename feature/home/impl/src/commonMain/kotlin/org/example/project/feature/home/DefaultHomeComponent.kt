package org.example.project.feature.home

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
import org.example.project.core.component.MoleculeComponent
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class DefaultHomeComponent(
    @Assisted componentContext: ComponentContext
) : HomeComponent, MoleculeComponent<HomeComponent.State, HomeComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): HomeComponent.State {
        var counter by rememberSaveable { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            while (isActive) {
                delay(1.seconds)
                counter++
            }
        }

        CollectEvents { event ->
            when (event) {
                HomeComponent.Event.IncrementClicked -> {
                    counter++
                }
            }
        }

        return HomeComponent.State(counter = counter)
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : HomeComponent.Factory {
        override fun create(
            componentContext: ComponentContext
        ): DefaultHomeComponent
    }
}
