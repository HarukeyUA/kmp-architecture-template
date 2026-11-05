package org.example.project.server.storage

/**
 * S3-compatible object-storage connection parameters, resolved from the `S3_*` environment when
 * something first injects [BlobStore]. [forcePathStyle] defaults to `true` because RustFS and most
 * S3-compatible stores address the bucket in the URL path rather than the host.
 */
data class StorageConfig(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val forcePathStyle: Boolean = true,
) {
    // Redact the credentials so they can't leak into a log line or crash report via the data-class
    // default toString.
    override fun toString(): String =
        "StorageConfig(endpoint=$endpoint, region=$region, bucket=$bucket, " +
            "accessKey=***, secretKey=***, forcePathStyle=$forcePathStyle)"
}
