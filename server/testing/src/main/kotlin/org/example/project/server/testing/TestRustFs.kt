package org.example.project.server.testing

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.storage.StorageConfig

/**
 * Process-wide RustFS Testcontainer for integration tests that exercise the bucket protocol
 * end-to-end; each should call [resetBucket] to start clean. `serverTest(environment =
 * TestRustFs.environment())` boots the graph with its `BlobStore`/S3 client pointed here.
 */
object TestRustFs {
    private const val BUCKET_NAME = "template-integration"
    private const val REGION = "us-east-1"

    private val container: RustFsContainer by lazy {
        val c = RustFsContainer().withReuse(false)
        c.start()
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { c.stop() } })
        runBlocking { ensureBucketExists(c) }
        c
    }

    /** The `S3_*` values the graph's storage provider resolves against. */
    fun environment(): ServerEnvironment {
        val c = container
        val values =
            mapOf(
                "S3_ENDPOINT" to c.s3Url,
                "S3_REGION" to REGION,
                "S3_BUCKET" to BUCKET_NAME,
                "S3_ACCESS_KEY" to c.accessKey,
                "S3_SECRET_KEY" to c.secretKey,
            )
        return ServerEnvironment { values[it] }
    }

    private fun storageConfig(): StorageConfig = storageConfig(container)

    private fun storageConfig(c: RustFsContainer): StorageConfig =
        StorageConfig(
            bucket = BUCKET_NAME,
            endpoint = c.s3Url,
            region = REGION,
            accessKey = c.accessKey,
            secretKey = c.secretKey,
        )

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
        val cfg = storageConfig(c)
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
