package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandlerOwner

@Composable
context(backHandlerOwner: BackHandlerOwner)
fun HandleBack(
    isEnabled: Boolean = true,
    priority: Int = 0,
    onBackStarted: ((BackEvent) -> Unit)? = null,
    onBackProgressed: ((BackEvent) -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val onBackStartedState = rememberUpdatedState(onBackStarted)
    val onBackProgressedState = rememberUpdatedState(onBackProgressed)
    val onBackCancelledState = rememberUpdatedState(onBackCancelled)
    val onBackState = rememberUpdatedState(onBack)

    // The callback outlives every recomposition, so each handler reads its lambda when the gesture
    // arrives; capturing them at construction would pin the first composition's closures forever.
    val callback = remember {
        BackCallback(
            isEnabled = isEnabled,
            priority = priority,
            onBackStarted = { event -> onBackStartedState.value?.invoke(event) },
            onBackProgressed = { event -> onBackProgressedState.value?.invoke(event) },
            onBackCancelled = { onBackCancelledState.value?.invoke() },
            onBack = { onBackState.value() },
        )
    }

    SideEffect { callback.isEnabled = isEnabled }
    SideEffect { callback.priority = priority }

    DisposableEffect(callback) {
        backHandlerOwner.backHandler.register(callback)

        onDispose { backHandlerOwner.backHandler.unregister(callback) }
    }
}
