package org.example.project.core.component

import com.arkivanov.decompose.ComponentContext

/**
 * Component that handles events but does not produce reactive UI state.
 *
 * Use this for coordinator/router components that:
 * - React to UI events (navigation, actions)
 * - Delegate state production to child components
 * - May expose navigation state (stacks, slots) managed by Decompose
 *
 * Example: A flow coordinator that handles "next/back" events but shows child screens.
 */
interface EventComponent<E : UiEvent> : ComponentContext {
    fun onEvent(event: E)
}
