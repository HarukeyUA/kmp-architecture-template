package org.example.project.server.testing

import dev.zacsweers.metro.createGraphFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.example.project.server.ServerConfig
import org.example.project.server.ServerGraph
import org.example.project.server.auth.JwtConfig
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.observability.MetricsConfig
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

/** A metrics port of 0 — the test engine never binds the second connector anyway. */
fun testMetricsConfig(): MetricsConfig = MetricsConfig(port = 0)

/**
 * Builds a full [ServerConfig] around [databaseConfig] and [storageConfig] (throwaway storage by
 * default for graph-building tests that don't touch object storage).
 */
fun testServerConfig(
    databaseConfig: DatabaseConfig,
    storageConfig: StorageConfig = testStorageConfig(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
): ServerConfig =
    ServerConfig(
        host = "localhost",
        port = 0,
        version = "test",
        database = databaseConfig,
        storage = storageConfig,
        metrics = testMetricsConfig(),
        webLimits = webLimitsConfig,
        jwt = jwtConfig,
    )

/**
 * Builds the real [ServerGraph] around [databaseConfig], threading the sub-configs like `main`. A
 * blob-exercising test passes a MinIO-backed [storageConfig] so presign/HEAD/delete legs hit a real
 * S3-compatible store.
 */
fun buildTestGraph(
    databaseConfig: DatabaseConfig,
    storageConfig: StorageConfig = testStorageConfig(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
): ServerGraph {
    val config = testServerConfig(databaseConfig, storageConfig, webLimitsConfig, jwtConfig)
    return createGraphFactory<ServerGraph.Factory>()
        .create(
            config,
            databaseConfig,
            config.storage,
            config.metrics,
            config.webLimits,
            config.jwt,
        )
}
