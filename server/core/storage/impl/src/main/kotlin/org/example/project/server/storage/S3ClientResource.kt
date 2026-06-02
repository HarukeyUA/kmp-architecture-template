package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.atomic.AtomicReference
import org.example.project.server.lifecycle.ServerResource

/**
 * Lazy owner for the app's S3 client. The wrapper itself is graph-scoped and registered for
 * shutdown, while the expensive SDK client is still built only if a blob operation actually needs
 * it.
 */
@Inject
@SingleIn(AppScope::class)
class S3ClientResource(private val config: StorageConfig) : ServerResource {
    private val client = AtomicReference<S3Client?>()

    fun get(): S3Client =
        client.get()
            ?: synchronized(this) { client.get() ?: buildS3Client(config).also(client::set) }

    override suspend fun close() {
        client.getAndSet(null)?.close()
    }
}
