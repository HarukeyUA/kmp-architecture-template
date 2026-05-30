package org.example.project.core.component.snackbar

/**
 * Child implementation of [SnackbarHandler] that forms the hierarchical chain.
 *
 * When [showSnackbar] is called:
 * - If a local host is registered at this level, it handles the message.
 * - Otherwise, the message bubbles up to the [parent] handler.
 */
class ChildSnackbarHandler(private val parent: SnackbarHandler) : SnackbarHandler {
    private var host: SnackbarHostCallback? = null

    override fun showSnackbar(message: SnackbarMessage) {
        val localHost = host
        if (localHost != null) {
            localHost.onShowSnackbar(message)
        } else {
            parent.showSnackbar(message)
        }
    }

    override fun registerHost(callback: SnackbarHostCallback) {
        host = callback
    }

    override fun unregisterHost(callback: SnackbarHostCallback) {
        if (host == callback) host = null
    }
}
