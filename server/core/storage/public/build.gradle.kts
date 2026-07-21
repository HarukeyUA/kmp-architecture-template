plugins {
    alias(libs.plugins.convention.server.core.public)
}

// BlobStore + StorageConfig are plain contracts (presigned-URL strings, sizes,
 // kotlin.time.Duration)
// — no S3 SDK on the public surface, so domains depend on the interface, never the implementation.
