package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.component.internal.EssentyLifecycleOwner
import org.example.project.core.component.internal.returningCompositionLocalProvider

/**
 * Default implementation of StatefulComponent using Molecule for state production.
 *
 * Provides:
 * - Lifecycle-aware coroutine scope
 * - Molecule-powered state production with @Composable
 * - StateKeeper integration for saved state that survives process death on Android and iOS
 * - Essenty Lifecycle -> AndroidX LifecycleOwner bridge
 *
 * User actions are handled by `eventSink` lambdas built inside [produceState] and carried by the
 * returned state; each lambda closes over the component's callbacks, [scope], and `remember`ed
 * Compose state.
 */
abstract class MoleculeComponent<S : UiState>(componentContext: AppComponentContext) :
    StatefulComponent<S>, AppComponentContext by componentContext {

    /**
     * Lifecycle-aware coroutine scope. Uses the platform main dispatcher and a SupervisorJob.
     * Automatically canceled when the component is destroyed.
     */
    protected val scope: CoroutineScope = lifecycleAwareScope()

    override val state: StateFlow<S> by lazy {
        scope.launchMolecule(
            mode = RecompositionMode.Immediate,
            snapshotNotifier = snapshotNotifier,
        ) {
            val lifecycleOwner = remember { EssentyLifecycleOwner(lifecycle) }

            returningCompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                ProvideStateKeeperSaveableStateRegistry { produceState() }
            }
        }
    }

    /** Implement this to produce your component's state. */
    @Composable protected abstract fun produceState(): S
}
