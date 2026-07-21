package org.example.project.core.component

import kotlinx.coroutines.flow.StateFlow

/**
 * Component that produces reactive UI state. User actions are delivered through `eventSink` lambdas
 * carried by the state itself, not through a method on the component (see ARCHITECTURE.md).
 */
interface StatefulComponent<S : UiState> : AppComponentContext {
    /** Observable state stream for UI consumption. */
    val state: StateFlow<S>
}
