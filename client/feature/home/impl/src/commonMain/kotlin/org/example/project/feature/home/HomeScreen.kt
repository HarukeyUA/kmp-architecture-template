package org.example.project.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.home.impl.Res
import org.example.project.client.feature.home.impl.home_log_out
import org.example.project.client.feature.home.impl.home_title
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeScreen : HomeScreen {
    @Composable
    override fun Content(component: HomeComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        HomeScreenContent(state = state)
    }
}

@Composable
internal fun HomeScreenContent(state: HomeComponent.State) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Button(
            onClick = { state.eventSink(HomeComponent.Event.LogoutClicked) },
            modifier = Modifier.testTag(HomeScreenTestTags.LOG_OUT),
        ) {
            Text(text = stringResource(Res.string.home_log_out))
        }
    }
}
