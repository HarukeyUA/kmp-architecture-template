package org.example.project.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.home.impl.Res
import org.example.project.client.feature.home.impl.home_title
import org.jetbrains.compose.resources.stringResource

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeScreen : HomeScreen {
    @Composable
    override fun Content(component: HomeComponent) {
        HomeScreenContent()
    }
}

@Composable
internal fun HomeScreenContent() {
    Box(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.home_title),
            modifier = Modifier.testTag(HomeScreenTestTags.TITLE),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
