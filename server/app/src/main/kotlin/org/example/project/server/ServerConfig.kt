package org.example.project.server

import kotlin.time.Duration.Companion.minutes
import org.example.project.server.auth.JwtConfig
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.PostgresUrl
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.required
import org.example.project.server.observability.MetricsConfig
import org.example.project.server.web.WebLimitsConfig

/**
 * The one typed configuration, loaded once at startup. Fail-fast is unconditional: no environment
 * gate and no dev default, so a missing `DATABASE_URL` or `JWT_SECRET` refuses the boot everywhere.
 * Object storage is absent on purpose: `S3_*` is read only if something injects `BlobStore`.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val version: String,
    val database: DatabaseConfig,
    val metrics: MetricsConfig,
    val webLimits: WebLimitsConfig,
    val jwt: JwtConfig,
) {
    companion object {
        private const val MIN_JWT_SECRET_LENGTH = 32

        fun load(environment: ServerEnvironment = ServerEnvironment.Process): ServerConfig {
            // Blank counts as missing everywhere: `JWT_SECRET=` fails fast like an absent one, and
            // `SERVER_PORT=` from an env-file stub means "use the default", not `"".toInt()`.
            fun optionalOrNull(key: String): String? = environment[key]?.takeIf { it.isNotBlank() }

            fun optional(key: String, default: String): String = optionalOrNull(key) ?: default

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

            val parsedUrl = PostgresUrl.parse(environment.required("DATABASE_URL"))
            val jwtSecret = environment.required("JWT_SECRET")
            check(jwtSecret.length >= MIN_JWT_SECRET_LENGTH) {
                "'JWT_SECRET' must be at least $MIN_JWT_SECRET_LENGTH characters (256 bits) " +
                    "for HS256"
            }

            return ServerConfig(
                host = optional("SERVER_HOST", "0.0.0.0"),
                port = optionalInt("SERVER_PORT", "8080"),
                version = optional("APP_VERSION", "dev"),
                database =
                    DatabaseConfig(
                        jdbcUrl = parsedUrl.jdbcUrl,
                        // For a `jdbc:` url whose host hands out the credentials separately.
                        username = parsedUrl.user ?: optionalOrNull("DATABASE_USER"),
                        password = parsedUrl.password ?: optionalOrNull("DATABASE_PASSWORD"),
                        maxPoolSize = optionalInt("DATABASE_MAX_POOL_SIZE", "10"),
                    ),
                // Served on a dedicated port that is NOT mapped to a public domain/ingress, so only
                // an in-network Prometheus can scrape it.
                metrics = MetricsConfig(port = optionalInt("METRICS_PORT", "8081")),
                webLimits =
                    WebLimitsConfig(
                        maxRequestBodyBytes = optionalLong("MAX_REQUEST_BODY_BYTES", "1048576"),
                        // Deliberately defaults to unset (socket address): behind a proxy that
                        // fails *loudly* (shared bucket → visible 429s), whereas trusting a
                        // client-forgeable header by default would fail *silently* (limiter
                        // bypassable per request). See WebLimitsConfig.
                        clientIpHeader = optionalOrNull("CLIENT_IP_HEADER"),
                        credentialRateLimit = optionalInt("CREDENTIAL_RATE_LIMIT_PER_MINUTE", "10"),
                    ),
                jwt =
                    JwtConfig(
                        secret = jwtSecret,
                        issuer = optional("JWT_ISSUER", "kmp-template"),
                        audience = optional("JWT_AUDIENCE", "kmp-template"),
                        accessTokenTtl = optionalInt("JWT_ACCESS_TTL_MINUTES", "15").minutes,
                    ),
            )
        }
    }
}
