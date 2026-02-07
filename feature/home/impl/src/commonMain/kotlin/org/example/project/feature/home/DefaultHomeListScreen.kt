package org.example.project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeListScreen : HomeListScreen {

    @Composable
    override fun Content(component: HomeListComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        HomeListScreenContent(state = state, onEvent = component::onEvent)
    }
}

@Composable
private fun HomeListScreenContent(
    state: HomeListComponent.State,
    onEvent: (HomeListComponent.Event) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .systemBarsPadding()
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable {
                            onEvent(HomeListComponent.Event.ItemClick(item.id))
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(text = ">", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
