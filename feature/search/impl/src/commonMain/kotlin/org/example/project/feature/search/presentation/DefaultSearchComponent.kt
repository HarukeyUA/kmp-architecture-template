package org.example.project.feature.search.presentation

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.MoleculeComponent

@AssistedInject
class DefaultSearchComponent(@Assisted componentContext: AppComponentContext) :
    SearchComponent, MoleculeComponent<SearchComponent.State>(componentContext) {

    @Composable
    override fun produceState(): SearchComponent.State {
        val query = rememberTextFieldState()
        var results by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var isSearching by rememberSaveable { mutableStateOf(false) }

        return SearchComponent.State(
            queryTextFieldState = query,
            results = results,
            isSearching = isSearching,
            eventSink = { event ->
                when (event) {
                    SearchComponent.Event.SearchClicked -> {
                        if (!isSearching && query.text.isNotBlank()) {
                            isSearching = true
                            scope.launch {
                                // Simulate search delay
                                delay(500)
                                results =
                                    listOf(
                                        "Result 1 for '${query.text}'",
                                        "Result 2 for '${query.text}'",
                                        "Result 3 for '${query.text}'",
                                        "Result 4 for '${query.text}'",
                                        "Result 5 for '${query.text}'",
                                    )
                                isSearching = false
                            }
                        }
                    }
                }
            },
        )
    }

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    fun interface Factory : SearchComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultSearchComponent
    }
}
