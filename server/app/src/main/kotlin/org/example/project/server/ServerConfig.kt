package org.example.project.server

import org.example.project.server.database.DatabaseConfig

/**
 * The one typed, fail-fast configuration, loaded once at startup (ADR — § Local dev & config).
 *
 * Localhost defaults let the server run out of the box (`./gradlew :server:app:run` against the
 * compose Postgres). In production (`APP_ENV=production`) the DB secrets are **required** — a
 * missing one fails fast at boot rather than silently falling back to a dev default. There are no
 * scattered `System.getenv` calls anywhere else.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val version: String,
    val database: DatabaseConfig,
) {
    companion object {
        fun load(getenv: (String) -> String? = System::getenv): ServerConfig {
            val production = getenv("APP_ENV").equals("production", ignoreCase = true)

            fun required(key: String, devDefault: String): String =
                getenv(key)
                    ?: if (production) {
                        error("Missing required configuration '$key' (APP_ENV=production)")
                    } else {
                        devDefault
                    }

            fun optional(key: String, default: String): String = getenv(key) ?: default

            return ServerConfig(
                host = optional("SERVER_HOST", "0.0.0.0"),
                port = optional("SERVER_PORT", "8080").toInt(),
                version = optional("APP_VERSION", "dev"),
                database =
                    DatabaseConfig(
                        jdbcUrl = required("DATABASE_URL", "jdbc:postgresql://localhost:5432/app"),
                        username = required("DATABASE_USER", "app"),
                        password = required("DATABASE_PASSWORD", "app"),
                        maxPoolSize = optional("DATABASE_MAX_POOL_SIZE", "10").toInt(),
                    ),
            )
        }
    }
}
