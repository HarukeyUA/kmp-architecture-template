package org.example.project.server

import kotlin.time.Duration.Companion.minutes
import org.example.project.server.auth.JwtConfig
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.observability.MetricsConfig
import org.example.project.server.storage.StorageConfig
import org.example.project.server.web.WebLimitsConfig

/**
 * The one typed, fail-fast configuration, loaded once at startup (ADR — § Local dev & config).
 *
 * Localhost defaults let the server run out of the box (`./gradlew :server:app:run` against the
 * compose Postgres + MinIO). In production (`APP_ENV=production`) the DB and object-storage secrets
 * are **required** — a missing one fails fast at boot rather than silently falling back to a dev
 * default. There are no scattered `System.getenv` calls anywhere else.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val version: String,
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val metrics: MetricsConfig,
    val webLimits: WebLimitsConfig,
    val jwt: JwtConfig,
) {
    companion object {
        private const val MIN_JWT_SECRET_LENGTH = 32

        fun load(getenv: (String) -> String? = System::getenv): ServerConfig {
            val production = getenv("APP_ENV").equals("production", ignoreCase = true)

            // Treat a blank value as missing everywhere: a present-but-empty secret (e.g.
            // `S3_SECRET_KEY=`) must fail fast at boot like an absent one, and a present-but-empty
            // optional (e.g. `SERVER_PORT=` from an env-file stub) means "use the default", not
            // `"".toInt()`.
            fun required(key: String, devDefault: String): String =
                getenv(key)?.takeIf { it.isNotBlank() }
                    ?: if (production) {
                        error("Missing required configuration '$key' (APP_ENV=production)")
                    } else {
                        devDefault
                    }

            fun optional(key: String, default: String): String =
                getenv(key)?.takeIf { it.isNotBlank() } ?: default

            // Parse failures must name the offending key — a bare NumberFormatException at boot
            // would undercut the fail-fast-with-a-clear-message contract above.
            fun optionalInt(key: String, default: String): Int =
                optional(key, default).let {
                    it.toIntOrNull() ?: error("Configuration '$key' must be an integer, got '$it'")
                }

            fun optionalLong(key: String, default: String): Long =
                optional(key, default).let {
                    it.toLongOrNull() ?: error("Configuration '$key' must be an integer, got '$it'")
                }

            return ServerConfig(
                host = optional("SERVER_HOST", "0.0.0.0"),
                port = optionalInt("SERVER_PORT", "8080"),
                version = optional("APP_VERSION", "dev"),
                database =
                    DatabaseConfig(
                        jdbcUrl = required("DATABASE_URL", "jdbc:postgresql://localhost:5432/app"),
                        username = required("DATABASE_USER", "app"),
                        password = required("DATABASE_PASSWORD", "app"),
                        maxPoolSize = optionalInt("DATABASE_MAX_POOL_SIZE", "10"),
                    ),
                storage =
                    StorageConfig(
                        // Localhost defaults match the compose MinIO; prod must supply real values.
                        endpoint = required("S3_ENDPOINT", "http://localhost:9000"),
                        region = optional("S3_REGION", "us-east-1"),
                        bucket = required("S3_BUCKET", "blobs"),
                        accessKey = required("S3_ACCESS_KEY", "minio"),
                        secretKey = required("S3_SECRET_KEY", "minio12345"),
                    ),
                // Served on a dedicated port that is NOT mapped to a public domain/ingress, so only
                // an in-network Prometheus can scrape it. Optional with a dev default like the
                // port.
                metrics = MetricsConfig(port = optionalInt("METRICS_PORT", "8081")),
                webLimits =
                    WebLimitsConfig(
                        maxRequestBodyBytes = optionalLong("MAX_REQUEST_BODY_BYTES", "1048576"),
                        // Deliberately defaults to unset (socket address): behind a proxy that
                        // fails *loudly* (shared bucket → visible 429s), whereas trusting a
                        // client-forgeable header by default would fail *silently* (limiter
                        // bypassable per request). See WebLimitsConfig.
                        clientIpHeader = getenv("CLIENT_IP_HEADER")?.takeIf { it.isNotBlank() },
                        credentialRateLimit = optionalInt("CREDENTIAL_RATE_LIMIT_PER_MINUTE", "10"),
                    ),
                jwt =
                    JwtConfig(
                        // HS256 signing secret. The dev default is deliberately unusable in
                        // production: it's required there, and a short (guessable-entropy) value
                        // fails fast too.
                        secret =
                            required("JWT_SECRET", "dev-only-jwt-secret-0123456789abcdef").also {
                                if (production && it.length < MIN_JWT_SECRET_LENGTH) {
                                    error(
                                        "'JWT_SECRET' must be at least $MIN_JWT_SECRET_LENGTH " +
                                            "characters (256 bits) for HS256"
                                    )
                                }
                            },
                        issuer = optional("JWT_ISSUER", "kmp-template"),
                        audience = optional("JWT_AUDIENCE", "kmp-template"),
                        accessTokenTtl = optionalInt("JWT_ACCESS_TTL_MINUTES", "15").minutes,
                    ),
            )
        }
    }
}
