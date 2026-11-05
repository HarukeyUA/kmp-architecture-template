package org.example.project.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.EmbeddedComponent
import org.example.project.core.component.MoleculeComponent
import org.example.project.core.component.UiState

/**
 * Hosts a single [EmbeddedComponent] in a molecule of its own, so unit tests can observe it through
 * a `state: StateFlow`. [setInput] recomposes the child with a new input the way the owning screen
 * component would. Give the host and the child separate component contexts: sharing one registers
 * two saveable-state registries on the same StateKeeper key.
 *
 * Drive input changes under the `Queued` test main mode. If the child keys an effect on the input,
 * `Eager`'s unconfined dispatch relaunches that effect re-entrantly inside the molecule's frame
 * loop and stalls it.
 */
class EmbeddedComponentTestHost<I, S : UiState>(
    componentContext: AppComponentContext,
    initialInput: I,
    private val child: EmbeddedComponent<I, S>,
) : MoleculeComponent<S>(componentContext) {

    private val input = mutableStateOf(initialInput)

    fun setInput(value: I) {
        // An applied mutable snapshot notifies apply observers synchronously, so the host
        // recomposes deterministically under both test main modes; a bare global write would depend
        // on the molecule's write-observer coroutine getting scheduled.
        Snapshot.withMutableSnapshot { input.value = value }
    }

    @Composable override fun produceState(): S = child.present(input.value)
}
