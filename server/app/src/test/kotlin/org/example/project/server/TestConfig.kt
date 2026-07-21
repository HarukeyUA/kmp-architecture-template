package org.example.project.server

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.example.project.server.auth.JwtConfig
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
 * A [JwtConfig] with the production-default TTL for integration flows. A test probing access-token
 * expiry itself can pass a tighter [accessTokenTtl].
 */
fun testJwtConfig(accessTokenTtl: Duration = 15.minutes): JwtConfig =
    JwtConfig(
        secret = "test-only-jwt-secret-0123456789abcdef",
        issuer = "kmp-template-test",
        audience = "kmp-template-test",
        accessTokenTtl = accessTokenTtl,
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
