package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.example.project.server.lifecycle.ServerResource

@ContributesTo(AppScope::class)
interface StorageProviders {
    /**
     * The single [S3Client]. `@SingleIn` + Metro's lazy provision means it is built only when a
     * domain first injects [BlobStore]; a blob-less server never constructs it, so object storage
     * is never a boot-time dependency. [StorageConfig] itself is provided by the graph factory,
     * threaded from the one typed `ServerConfig` (mirroring `DatabaseConfig`).
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideS3Client(resource: S3ClientResource): S3Client = resource.get()

    /** Registered eagerly, but closing it is a no-op unless the lazy client was actually built. */
    @Provides @IntoSet fun s3ClientResource(resource: S3ClientResource): ServerResource = resource
}
