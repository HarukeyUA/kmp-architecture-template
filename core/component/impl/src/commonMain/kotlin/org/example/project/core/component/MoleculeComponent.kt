package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.example.project.core.component.internal.EssentyLifecycleOwner
import org.example.project.core.component.internal.ProvideStateKeeperSaveableStateRegistry
import org.example.project.core.component.internal.moleculeContext
import org.example.project.core.component.internal.returningCompositionLocalProvider

/**
 * Default implementation of StatefulComponent using Molecule for state production.
 *
 * Provides:
 * - Lifecycle-aware coroutine scope
 * - Molecule-powered state production with @Composable
 * - StateKeeper integration for saved state that survives process death on Android and iOS
 * - Event channel for UI -> Component communication
 * - Essenty Lifecycle -> AndroidX LifecycleOwner bridge
 */
abstract class MoleculeComponent<S : UiState, E : UiEvent>(
    componentContext: ComponentContext,
) : StatefulComponent<S, E>, ComponentContext by componentContext {

    /**
     * Lifecycle-aware coroutine scope.
     * Uses platform-specific dispatcher via moleculeContext().
     * Automatically cancelled when component is destroyed.
     */
    protected val scope: CoroutineScope = coroutineScope(moleculeContext() + SupervisorJob())

    /** Event channel with buffer to prevent event loss */
    private val events = MutableSharedFlow<E>(extraBufferCapacity = 64)

    override val state: StateFlow<S> = scope.launchMolecule(mode = RecompositionMode.Immediate) {
        val lifecycleOwner = remember { EssentyLifecycleOwner(lifecycle) }

        returningCompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
            ProvideStateKeeperSaveableStateRegistry {
                produceState()
            }
        }
    }

    override fun onEvent(event: E) {
        scope.launch {
            events.emit(event)
        }
    }

    /**
     * Collect events within the Molecule composition.
     * Use this in produceState() to handle UI events.
     */
    @Composable
    protected fun CollectEvents(onEvent: suspend (E) -> Unit) {
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            events.collectLatest {
                coroutineScope.launch {
                    onEvent(it)
                }
            }
        }
    }

    /**
     * Implement this to produce your component's state.
     */
    @Composable
    protected abstract fun produceState(): S
}
