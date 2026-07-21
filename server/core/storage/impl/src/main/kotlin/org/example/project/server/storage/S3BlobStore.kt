package org.example.project.server.storage

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration

/**
 * [BlobStore] backed by [aws-sdk-kotlin](https://github.com/awslabs/aws-sdk-kotlin) — the same
 * client speaks to MinIO (local) and Railway Buckets / AWS S3 (prod), since both are S3-compatible.
 *
 * Presigning is a local signing operation: it never touches the network, so it stays cheap and
 * doesn't make object storage a boot-time dependency (the [S3Client] is created lazily by Metro the
 * first time a domain injects [BlobStore], so a blob-less server never opens an S3 connection).
 */
@Inject
@ContributesBinding(AppScope::class)
class S3BlobStore(private val client: S3Client, private val config: StorageConfig) : BlobStore {

    override suspend fun presignPut(key: String, contentLengthBytes: Long, ttl: Duration): String {
        // Fail at the call site with a clear message rather than letting a 0/negative length or a
        // non-positive TTL surface later as an opaque S3 signing/validation error.
        require(contentLengthBytes > 0) {
            "contentLengthBytes must be positive: $contentLengthBytes"
        }
        require(ttl.isPositive()) { "ttl must be positive: $ttl" }
        // contentLength is baked into the signed headers, so the store rejects a PUT whose actual
        // Content-Length differs — the upload size cap is enforced by S3, not trusted from the
        // client.
        val request = PutObjectRequest {
            bucket = config.bucket
            this.key = key
            contentLength = contentLengthBytes
        }
        return client.presignPutObject(request, ttl).url.toString()
    }

    override suspend fun presignGet(key: String, ttl: Duration): String {
        require(ttl.isPositive()) { "ttl must be positive: $ttl" }
        val request = GetObjectRequest {
            bucket = config.bucket
            this.key = key
        }
        return client.presignGetObject(request, ttl).url.toString()
    }

    override suspend fun headSize(key: String): Long? =
        try {
            client
                .headObject(
                    HeadObjectRequest {
                        bucket = config.bucket
                        this.key = key
                    }
                )
                .contentLength
        } catch (_: NotFound) {
            null
        }

    override suspend fun delete(key: String) {
        // S3 DELETE is idempotent: a missing object returns 204, not 404, so there is nothing to
        // swallow.
        client.deleteObject(
            DeleteObjectRequest {
                bucket = config.bucket
                this.key = key
            }
        )
    }
}
