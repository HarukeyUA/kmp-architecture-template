package org.example.project.feature.search

import androidx.compose.runtime.Composable
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
import org.example.project.core.component.MoleculeComponent

@AssistedInject
class DefaultSearchComponent(
    @Assisted componentContext: ComponentContext,
) : SearchComponent, MoleculeComponent<SearchComponent.State, SearchComponent.Event>(componentContext) {

    @Composable
    override fun produceState(): SearchComponent.State {
        var query by rememberSaveable { mutableStateOf("") }
        var results by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var isSearching by rememberSaveable { mutableStateOf(false) }

        CollectEvents { event ->
            when (event) {
                is SearchComponent.Event.QueryChanged -> {
                    query = event.query
                }
                SearchComponent.Event.SearchClicked -> {
                    if (query.isNotBlank()) {
                        isSearching = true
                        // Simulate search delay
                        delay(500)
                        results = listOf(
                            "Result 1 for '$query'",
                            "Result 2 for '$query'",
                            "Result 3 for '$query'",
                            "Result 4 for '$query'",
                            "Result 5 for '$query'"
                        )
                        isSearching = false
                    }
                }
            }
        }

        return SearchComponent.State(
            query = query,
            results = results,
            isSearching = isSearching
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SearchComponent.Factory {
        override fun create(componentContext: ComponentContext): DefaultSearchComponent
    }
}
