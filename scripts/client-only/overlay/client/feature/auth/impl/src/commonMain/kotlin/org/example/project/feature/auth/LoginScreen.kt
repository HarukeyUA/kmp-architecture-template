package org.example.project.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.ui.error.message

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
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Welcome", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.email,
            onValueChange = { state.eventSink(LoginComponent.Event.EmailChanged(it)) },
            label = { Text(text = "Email") },
            singleLine = true,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { state.eventSink(LoginComponent.Event.PasswordChanged(it)) },
            label = { Text(text = "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { Text(text = it.message(), color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { state.eventSink(LoginComponent.Event.LoginClicked) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Log in")
        }

        TextButton(
            onClick = { state.eventSink(LoginComponent.Event.SignupClicked) },
            enabled = !state.isSubmitting,
        ) {
            Text(text = "Create account")
        }

        if (state.isSubmitting) {
            CircularProgressIndicator()
        }
    }
}
