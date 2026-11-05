package org.example.project.server.testing

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import org.example.project.server.storage.StorageConfig
import org.example.project.server.storage.testing.RustFsContainer

/**
 * Process-wide RustFS Testcontainer for integration tests that exercise the bucket protocol
 * end-to-end (a domain that owns blobs — an avatar feature, say — presigning/HEADing/deleting
 * against a real S3-compatible store). Mirrors [TestPostgres]: one container reused across the JVM,
 * lazy-started, shut down by a JVM hook. Each test should call [resetBucket] (or use a fresh bucket
 * name) to start clean.
 *
 * [storageConfig] is the graph factory's [StorageConfig] pointed at this container — a
 * `serverTest(storageConfig = TestRustFs.storageConfig())` boots the `:server:app` graph with its
 * `BlobStore`/S3 client built from it. Suites that never touch the bucket leg keep the default
 * throwaway [testStorageConfig]; the S3 client is lazy, so nothing connects.
 */
object TestRustFs {
    private const val BUCKET_NAME = "template-integration"

    private val container: RustFsContainer by lazy {
        val c = RustFsContainer().withReuse(false)
        c.start()
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { c.stop() } })
        runBlocking { ensureBucketExists(c) }
        c
    }

    fun storageConfig(): StorageConfig {
        val c = container
        return StorageConfig(
            bucket = BUCKET_NAME,
            endpoint = c.s3Url,
            region = "us-east-1",
            accessKey = c.accessKey,
            secretKey = c.secretKey,
        )
    }

    /** Empties the bucket between tests. */
    fun resetBucket() {
        val cfg = storageConfig()
        runBlocking {
            adminClient(cfg).use { client ->
                val list = client.listObjectsV2 { bucket = cfg.bucket }
                list.contents?.forEach { obj ->
                    client.deleteObject {
                        bucket = cfg.bucket
                        key = obj.key
                    }
                }
            }
        }
    }

    private suspend fun ensureBucketExists(c: RustFsContainer) {
        val cfg =
            StorageConfig(
                bucket = BUCKET_NAME,
                endpoint = c.s3Url,
                region = "us-east-1",
                accessKey = c.accessKey,
                secretKey = c.secretKey,
            )
        adminClient(cfg).use { client ->
            try {
                client.createBucket { bucket = cfg.bucket }
            } catch (e: S3Exception) {
                // Bucket already exists from a previous run with reuse enabled — ignore.
                if (!e.message.contains("already")) throw e
            }
        }
    }

    private fun adminClient(cfg: StorageConfig): S3Client = S3Client {
        region = cfg.region
        endpointUrl = Url.parse(cfg.endpoint)
        forcePathStyle = true
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = cfg.accessKey
            secretAccessKey = cfg.secretKey
        }
    }
}
