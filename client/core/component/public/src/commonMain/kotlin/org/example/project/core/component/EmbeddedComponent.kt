package org.example.project.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import org.example.project.core.component.internal.EssentyLifecycleOwner
import org.example.project.core.component.internal.returningCompositionLocalProvider

/**
 * A section within a screen, running inside its owner's composition instead of producing a
 * `StateFlow` of its own: the owner calls [present] from its own `produceState` and carries the
 * result in its own `State`, so the child's frame lands atomically with the parent's.
 *
 * Still a full component: give it its own `childContext` so it keeps its own saved state and
 * lifecycle.
 *
 * [I] is what the owner supplies each frame, [Unit] when the child is self-contained.
 * Construction-time facts (callbacks, ids) belong on the factory instead.
 */
interface EmbeddedComponent<I, S : UiState> : AppComponentContext {

    /**
     * Produces this component's state for the owner's current frame.
     *
     * Call from exactly one place in the owner's `produceState`. If the call site is conditional,
     * wrap it in `key(...)` so the child's `remember`/`rememberSaveable` slots survive the branches
     * around it moving.
     */
    @Composable fun present(input: I): S
}

/** [EmbeddedComponent.present] without the [Unit] ceremony for input-less components. */
@Composable fun <S : UiState> EmbeddedComponent<Unit, S>.present(): S = present(Unit)

/**
 * Default implementation of [EmbeddedComponent]. It borrows the hosting molecule's recomposition
 * clock but gives the component the same plumbing [MoleculeComponent] does: a lifecycle-bound
 * [scope], its own lifecycle for Compose lifecycle-aware APIs, and its own StateKeeper.
 */
abstract class MoleculeEmbeddedComponent<I, S : UiState>(componentContext: AppComponentContext) :
    EmbeddedComponent<I, S>, AppComponentContext by componentContext {

    /** Cancelled automatically when the component is destroyed. */
    protected val scope: CoroutineScope = lifecycleAwareScope()

    @Composable
    final override fun present(input: I): S {
        val lifecycleOwner = remember { EssentyLifecycleOwner(lifecycle) }

        return returningCompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
            ProvideStateKeeperSaveableStateRegistry { produceState(input) }
        }
    }

    @Composable protected abstract fun produceState(input: I): S
}
