package org.example.project.server.database

import java.net.URI
import java.net.URISyntaxException

/** A JDBC url plus any credentials lifted out of it. Null creds ride inside [jdbcUrl] itself. */
data class PostgresUrlParts(val jdbcUrl: String, val user: String?, val password: String?) {
    // Redact the password so it can't leak into a log line or crash report.
    override fun toString(): String =
        "PostgresUrlParts(jdbcUrl=$jdbcUrl, user=${user ?: "<from url>"}, password=***)"
}

/**
 * Parses the `postgres://user:pass@host:port/db` form hosting platforms inject into a JDBC url with
 * the credentials lifted out, the shape HikariCP needs. A url already in `jdbc:` form is returned
 * verbatim; the driver reads any embedded credentials itself.
 */
object PostgresUrl {
    private val SCHEMES = setOf("postgres", "postgresql")
    private const val MALFORMED =
        "DATABASE_URL must be jdbc:postgresql://… or postgres://user:password@host:port/db"

    fun parse(raw: String): PostgresUrlParts {
        if (raw.startsWith("jdbc:")) return PostgresUrlParts(raw, user = null, password = null)
        val uri = toUriOrNull(raw) ?: throw IllegalArgumentException(MALFORMED)
        // A host URI can't parse (an underscore, say) leaves it null along with the user info.
        val host = uri.host
        require(uri.scheme in SCHEMES && host != null) { MALFORMED }
        val userInfo = uri.userInfo?.split(":", limit = 2)
        val hostPort = if (uri.port != -1) "$host:${uri.port}" else host
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return PostgresUrlParts(
            jdbcUrl = "jdbc:postgresql://$hostPort$path$query",
            user = userInfo?.getOrNull(0),
            password = userInfo?.getOrNull(1),
        )
    }

    // The URI's own failure quotes its input, which here carries the password.
    private fun toUriOrNull(raw: String): URI? =
        try {
            URI(raw)
        } catch (_: URISyntaxException) {
            null
        }
}
