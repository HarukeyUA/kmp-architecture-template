package org.example.project.core.component.snackbar

import androidx.compose.material3.SnackbarDuration as ComposeSnackbarDuration
import androidx.compose.material3.SnackbarHostState as ComposeSnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.example.project.core.component.AppComponentContext
import org.jetbrains.compose.resources.getString

/**
 * A [SnackbarHostCallback] that exposes received messages as a [SharedFlow]. Host components
 * register it with their [SnackbarHandler] and expose [messages] for their screen to display
 * through Compose's SnackbarHost.
 */
class SnackbarHostState : SnackbarHostCallback {
    private val _messages = MutableSharedFlow<SnackbarMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<SnackbarMessage> = _messages.asSharedFlow()

    override fun onShowSnackbar(message: SnackbarMessage) {
        _messages.tryEmit(message)
    }
}

/**
 * Registers this component as a snackbar host and returns a [SnackbarHostState] whose
 * [messages][SnackbarHostState.messages] flow the screen can observe. The host is unregistered when
 * the component's lifecycle is destroyed.
 */
fun AppComponentContext.snackbarHost(): SnackbarHostState {
    val state = SnackbarHostState()
    snackbarHandler.registerHost(state)
    lifecycle.doOnDestroy { snackbarHandler.unregisterHost(state) }
    return state
}

@Composable
fun rememberDispatchedSnackbarHostState(state: SnackbarHostState): ComposeSnackbarHostState {
    val composeSnackbarHostState = remember { ComposeSnackbarHostState() }

    LaunchedEffect(state) {
        state.messages.collect { message ->
            val result =
                composeSnackbarHostState.showSnackbar(
                    message = getString(message.text),
                    actionLabel = message.action?.let { getString(it.label) },
                    duration =
                        when (message.duration) {
                            SnackbarDuration.Short -> ComposeSnackbarDuration.Short
                            SnackbarDuration.Long -> ComposeSnackbarDuration.Long
                            SnackbarDuration.Indefinite -> ComposeSnackbarDuration.Indefinite
                        },
                )
            if (result == SnackbarResult.ActionPerformed) {
                message.action?.onClick?.invoke()
            }
        }
    }
    return composeSnackbarHostState
}
