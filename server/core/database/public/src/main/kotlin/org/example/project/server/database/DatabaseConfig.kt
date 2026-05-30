package org.example.project.server.database

/** Connection settings for the primary Postgres datasource, resolved once from [ServerConfig]. */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10,
)
