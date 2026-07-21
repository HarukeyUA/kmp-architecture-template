package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.example.project.server.lifecycle.ServerResource

/**
 * Lazy owner for the app's single S3 client. The wrapper is graph-scoped and registered for
 * shutdown, but the expensive SDK client is built only on the first [get] — so merely materializing
 * the `ServerResource` set at boot never opens an S3 connection, and a blob-less server never
 * builds one. `lazy` is `SYNCHRONIZED` by default, giving the same build-at-most-once guarantee the
 * graph's `@SingleIn` relies on.
 */
@Inject
@SingleIn(AppScope::class)
class S3ClientResource(private val config: StorageConfig) : ServerResource {
    private val client: Lazy<S3Client> = lazy { buildS3Client(config) }

    fun get(): S3Client = client.value

    /**
     * Closes the client only if it was actually built. Touching [client] unconditionally would
     * force the lazy and build a client purely to close it; the [Lazy.isInitialized] guard keeps
     * shutdown a true no-op when no blob op ever ran (honouring the [ServerResource] contract). A
     * forced `lazy` is terminal — unlike a resettable reference, a post-close [get] returns the
     * same already-closed client rather than silently resurrecting (and leaking) a new one past
     * shutdown.
     */
    override suspend fun close() {
        if (client.isInitialized()) client.value.close()
    }
}
