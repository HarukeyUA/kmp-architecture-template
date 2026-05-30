package org.example.project.core.component

/**
 * Marker interface for UI side effect types. Side effects are one-time events dispatched from a
 * component to the UI layer (e.g., scroll-to-top, show toast). Unlike [UiState], side effects are
 * not persisted and are consumed exactly once.
 */
interface UiSideEffect
