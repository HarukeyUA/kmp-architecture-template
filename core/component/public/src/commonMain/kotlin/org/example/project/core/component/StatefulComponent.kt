package org.example.project.core.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.StateFlow

/** Component that produces reactive UI state and handles events. */
interface StatefulComponent<S : UiState, E : UiEvent> : ComponentContext {
    /** Observable state stream for UI consumption. */
    val state: StateFlow<S>

    /** Send an event from UI to component. */
    fun onEvent(event: E)
}
