package org.example.project.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.splash.impl.Res
import org.example.project.client.feature.splash.impl.splash_session_unreadable
import org.example.project.client.feature.splash.impl.splash_sign_in_again
import org.example.project.client.feature.splash.impl.splash_try_again
import org.example.project.feature.splash.presentation.SplashComponent
import org.example.project.feature.splash.presentation.SplashScreen
import org.jetbrains.compose.resources.stringResource

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

        is SplashComponent.State.SessionUnreadable ->
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.splash_session_unreadable),
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { state.eventSink(SplashComponent.Event.RetryClicked) }) {
                    Text(text = stringResource(Res.string.splash_try_again))
                }
                TextButton(
                    onClick = { state.eventSink(SplashComponent.Event.SignInAgainClicked) },
                ) {
                    Text(text = stringResource(Res.string.splash_sign_in_again))
                }
            }
    }
}
