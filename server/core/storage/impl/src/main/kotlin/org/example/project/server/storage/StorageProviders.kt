package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.ServerResource
import org.example.project.server.lifecycle.required

@ContributesTo(AppScope::class)
interface StorageProviders {
    /**
     * Resolved when a domain first injects [BlobStore], so object storage is never a boot-time
     * dependency and a blob-less server needs no `S3_*` environment. A consumer on a server missing
     * any of them fails there, naming the key.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideStorageConfig(environment: ServerEnvironment): StorageConfig =
        StorageConfig(
            endpoint = environment.required("S3_ENDPOINT"),
            region = environment.required("S3_REGION"),
            bucket = environment.required("S3_BUCKET"),
            accessKey = environment.required("S3_ACCESS_KEY"),
            secretKey = environment.required("S3_SECRET_KEY"),
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideS3Client(resource: S3ClientResource): S3Client = resource.get()

    /** Registered eagerly, but closing it is a no-op unless the lazy client was actually built. */
    @Provides @IntoSet fun s3ClientResource(resource: S3ClientResource): ServerResource = resource
}
