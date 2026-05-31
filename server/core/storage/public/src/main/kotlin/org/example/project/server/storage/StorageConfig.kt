package org.example.project.server.storage

/**
 * S3-compatible object-storage connection parameters, part of the one typed [ServerConfig] (loaded
 * once, fail-fast on missing prod secrets — never scattered `System.getenv` calls). The localhost
 * defaults match the compose MinIO so blobs work out of the box.
 *
 * [forcePathStyle] is `true` by default because MinIO (and most S3-compatible stores) address the
 * bucket in the URL path rather than the host. Real AWS S3 also accepts path-style; flip it off
 * only if a provider requires virtual-hosted-style addressing.
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
