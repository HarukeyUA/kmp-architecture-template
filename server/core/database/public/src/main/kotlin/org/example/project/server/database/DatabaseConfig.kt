package org.example.project.server.database

/** Connection settings for the primary Postgres datasource, resolved once from [ServerConfig]. */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10,
) {
    // Redact the password so it can't leak into a log line or crash report via the data-class
    // default.
    override fun toString(): String =
        "DatabaseConfig(jdbcUrl=$jdbcUrl, username=$username, password=***, maxPoolSize=$maxPoolSize)"
}
