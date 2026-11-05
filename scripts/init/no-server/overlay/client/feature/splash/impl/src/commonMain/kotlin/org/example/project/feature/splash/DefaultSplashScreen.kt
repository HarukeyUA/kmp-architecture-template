package org.example.project.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.feature.splash.presentation.SplashComponent
import org.example.project.feature.splash.presentation.SplashScreen

@ContributesBinding(AppScope::class)
@Inject
class DefaultSplashScreen : SplashScreen {
    @Composable
    override fun Content(component: SplashComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        SplashScreenContent(state = state)
    }
}

@Composable
internal fun SplashScreenContent(state: SplashComponent.State) {
    when (state) {
        SplashComponent.State.Loading ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Splash",
                    textAlign = TextAlign.Center,
                )
            }
    }
}
