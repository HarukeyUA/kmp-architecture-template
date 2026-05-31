package org.example.project.server

import org.example.project.server.storage.StorageConfig

/**
 * A throwaway [StorageConfig] for graph-building in tests that don't touch object storage. The S3
 * client is provided lazily (`@SingleIn`), so nothing connects to these values — they only satisfy
 * the graph factory's `storageConfig` parameter.
 */
fun testStorageConfig(): StorageConfig =
    StorageConfig(
        endpoint = "http://localhost:9000",
        region = "us-east-1",
        bucket = "test",
        accessKey = "test",
        secretKey = "test",
    )
