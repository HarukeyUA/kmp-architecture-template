package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import org.example.project.core.component.internal.StateKeeperSaveableStateRegistry
import org.example.project.core.component.internal.returningCompositionLocalProvider

@Suppress("ComposableNaming")
@Composable
fun <T> AppComponentContext.ProvideStateKeeperSaveableStateRegistry(
    key: String = "state-keeper-state-registry",
    content: @Composable () -> T,
): T {
    val registry =
        remember(stateKeeper, key) { StateKeeperSaveableStateRegistry(stateKeeper, key = key) }

    DisposableEffect(registry) { onDispose { registry.unregister() } }

    return returningCompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        content()
    }
}
