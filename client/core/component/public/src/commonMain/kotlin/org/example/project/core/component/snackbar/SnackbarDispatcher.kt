package org.example.project.core.component.snackbar

/**
 * Root implementation of [SnackbarHandler].
 *
 * Messages that reach this level (no child handler intercepted them) are forwarded to the
 * registered host, if any. Messages without a host are silently dropped.
 */
class SnackbarDispatcher : SnackbarHandler {
    private var host: SnackbarHostCallback? = null

    override fun showSnackbar(message: SnackbarMessage) {
        host?.onShowSnackbar(message)
    }

    override fun registerHost(callback: SnackbarHostCallback) {
        host = callback
    }

    override fun unregisterHost(callback: SnackbarHostCallback) {
        if (host == callback) host = null
    }
}
