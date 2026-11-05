package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import com.arkivanov.essenty.instancekeeper.getOrCreate
import org.example.project.core.component.internal.StateKeeperSaveableStateRegistry
import org.example.project.core.component.internal.returningCompositionLocalProvider

/**
 * Routes every `rememberSaveable` in [content] into this component's saved state under [key].
 * Values last as long as the component, not the composition: a composition rebuilt over a live
 * component (an Android configuration change) and a branch that leaves and re-enters both get back
 * what was held.
 */
@Suppress("ComposableNaming") // Reads as a provider wrapping content, though it returns a value.
@Composable
fun <T> AppComponentContext.ProvideStateKeeperSaveableStateRegistry(
    key: String = "state-keeper-state-registry",
    content: @Composable () -> T,
): T {
    val registry =
        remember(instanceKeeper, stateKeeper, key) {
            instanceKeeper
                .getOrCreate(RegistryKey(key)) { StateKeeperSaveableStateRegistry(key) }
                .apply { attach(stateKeeper) }
        }

    return returningCompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        content()
    }
}

/** Keeps the registry's slot apart from whatever else the component keys by a bare string. */
private data class RegistryKey(val key: String)
