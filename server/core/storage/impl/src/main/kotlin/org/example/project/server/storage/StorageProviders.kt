package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

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
    fun provideS3Client(config: StorageConfig): S3Client = buildS3Client(config)
}
