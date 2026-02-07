package org.example.project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
class DefaultHomeDetailScreen : HomeDetailScreen {

    @Composable
    override fun Content(component: HomeDetailComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        HomeDetailScreenContent(state = state, onEvent = component::onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDetailScreenContent(
    state: HomeDetailComponent.State,
    onEvent: (HomeDetailComponent.Event) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(text = state.title) },
            navigationIcon = {
                TextButton(onClick = { onEvent(HomeDetailComponent.Event.BackClick) }) {
                    Text(text = "<")
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = state.title, style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = state.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
