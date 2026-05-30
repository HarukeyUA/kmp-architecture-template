package org.example.project.core.component

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Holds a buffered channel for dispatching one-time [UiSideEffect]s from a component to the UI.
 *
 * Usage in a [MoleculeComponent]:
 * ```
 * override val sideEffects = SideEffects<MyComponent.SideEffect>()
 *
 * // inside CollectEvents:
 * sideEffects.send(MyComponent.SideEffect.ScrollToTop)
 * ```
 */
class SideEffects<SE : UiSideEffect> {
    private val channel = Channel<SE>(capacity = Channel.BUFFERED)

    /** Flow of side effects. Collect with [CollectSideEffects] in the UI layer. */
    val flow: Flow<SE> = channel.receiveAsFlow()

    /** Emit a side effect. Non-suspending; safe to call from any context. */
    context(_: AppComponentContext)
    fun send(effect: SE) {
        channel.trySend(effect)
    }
}
