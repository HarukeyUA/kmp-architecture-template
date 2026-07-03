package org.example.project.feature.auth

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
class DefaultLoginScreen : LoginScreen {
    @Composable
    override fun Content(component: LoginComponent) {
        val state by component.state.collectAsStateWithLifecycle()

        LoginScreenContent(state = state)
    }
}

@Composable
internal fun LoginScreenContent(state: LoginComponent.State) {
    Column(
        modifier =
            Modifier.background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Counter: ${state.counter}", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { state.eventSink(LoginComponent.Event.LoginClicked) }) {
            Text(text = "Login")
        }
    }
}
