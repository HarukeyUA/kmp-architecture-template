package org.example.project.server

import org.example.project.server.storage.StorageConfig
import org.example.project.server.web.WebLimitsConfig

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

/**
 * Production-default [WebLimitsConfig] for tests that aren't about the limits themselves — roomy
 * enough that ordinary integration flows never trip them. The hardening test passes tight values
 * instead.
 */
fun testWebLimitsConfig(
    maxRequestBodyBytes: Long = 1_048_576L,
    clientIpHeader: String? = null,
    credentialRateLimit: Int = 100,
): WebLimitsConfig =
    WebLimitsConfig(
        maxRequestBodyBytes = maxRequestBodyBytes,
        clientIpHeader = clientIpHeader,
        credentialRateLimit = credentialRateLimit,
    )
