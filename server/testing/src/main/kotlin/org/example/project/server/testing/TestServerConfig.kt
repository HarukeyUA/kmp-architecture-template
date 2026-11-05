package org.example.project.server.testing

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.example.project.server.ServerConfig
import org.example.project.server.ServerGraph
import org.example.project.server.auth.JwtConfig
import org.example.project.server.createServerGraph
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.observability.MetricsConfig
import org.example.project.server.web.WebLimitsConfig

/**
 * An environment with nothing set, so a test can't pick up values from the host's shell. A graph
 * built on it fails loudly if anything asks for config the test didn't supply.
 */
fun testEnvironment(): ServerEnvironment = ServerEnvironment { null }

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

/** Builds a full [ServerConfig] around [databaseConfig]. */
fun testServerConfig(
    databaseConfig: DatabaseConfig,
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
): ServerConfig =
    ServerConfig(
        host = "localhost",
        port = 0,
        version = "test",
        database = databaseConfig,
        metrics = testMetricsConfig(),
        webLimits = webLimitsConfig,
        jwt = jwtConfig,
    )

/**
 * Builds the real [ServerGraph] around [databaseConfig]. A blob-exercising test passes
 * [TestRustFs.environment] so presign/HEAD/delete legs hit a real S3-compatible store.
 */
fun buildTestGraph(
    databaseConfig: DatabaseConfig,
    environment: ServerEnvironment = testEnvironment(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
): ServerGraph =
    createServerGraph(testServerConfig(databaseConfig, webLimitsConfig, jwtConfig), environment)
