package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.example.project.server.lifecycle.ServerResource

/**
 * Lazy owner for the app's single S3 client. The wrapper is graph-scoped and registered for
 * shutdown, but [config] and the SDK client resolve only on the first [get], so materializing the
 * `ServerResource` set at boot reads no `S3_*` value and opens no S3 connection.
 */
@Inject
@SingleIn(AppScope::class)
class S3ClientResource(config: Lazy<StorageConfig>) : ServerResource {
    // The client outlives its construction: [close] releases it at shutdown.
    @Suppress("MissingUseCall")
    private val client: Lazy<S3Client> = lazy { buildS3Client(config.value) }

    fun get(): S3Client = client.value

    /**
     * Closes the client only if it was built; otherwise shutdown is a no-op. A forced `lazy` is
     * terminal, so a post-close [get] returns the closed client rather than leaking a new one.
     */
    override suspend fun close() {
        if (client.isInitialized()) client.value.close()
    }
}
