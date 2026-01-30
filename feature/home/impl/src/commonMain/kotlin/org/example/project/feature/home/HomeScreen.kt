package org.example.project.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
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
class DefaultHomeScreen : HomeScreen {
    @Composable
    override fun Content(component: HomeComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        HomeScreenContent(state = state, onEvent = component::onEvent)
    }
}

@Composable
private fun HomeScreenContent(state: HomeComponent.State, onEvent: (HomeComponent.Event) -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Home", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Counter: ${state.counter}", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { onEvent(HomeComponent.Event.IncrementClicked) }) { Text(text = "+1") }
    }
}
