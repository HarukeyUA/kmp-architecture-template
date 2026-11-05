package org.example.project.server.lifecycle

/**
 * An app-scoped resource owned by the server graph and released when the application stops.
 *
 * Implementations should be idempotent: shutdown may race with partial startup, and lazy resources
 * should simply no-op when they were never acquired.
 */
fun interface ServerResource {
    suspend fun close()
}

/**
 * Closes every [ServerResource], preserving the first failure and attaching later failures as
 * suppressed exceptions.
 */
// A cancellation from one close must not skip the remaining resources: it is kept as the failure
// and rethrown once every resource has had its turn.
@Suppress("TooGenericExceptionCaught", "SuspendFunSwallowedCancellation")
suspend fun Iterable<ServerResource>.closeAll() {
    var failure: Throwable? = null
    for (resource in this) {
        try {
            resource.close()
        } catch (e: Throwable) {
            failure?.addSuppressed(e) ?: run { failure = e }
        }
    }
    failure?.let { throw it }
}
