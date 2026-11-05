package org.example.project.core.component.snackbar

/**
 * Hierarchical snackbar dispatch interface.
 *
 * Components use [showSnackbar] to display messages. The message bubbles up through the handler
 * hierarchy until it reaches a registered host (the nearest ancestor that called [registerHost]).
 *
 * Components that want to display snackbars (e.g., those with a Scaffold) register a
 * [SnackbarHostCallback] via [registerHost].
 */
interface SnackbarHandler {
    fun showSnackbar(message: SnackbarMessage)

    fun registerHost(callback: SnackbarHostCallback)

    fun unregisterHost(callback: SnackbarHostCallback)
}

fun interface SnackbarHostCallback {
    fun onShowSnackbar(message: SnackbarMessage)
}
