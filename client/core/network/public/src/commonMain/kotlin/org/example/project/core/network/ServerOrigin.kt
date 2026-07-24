package org.example.project.core.network

import io.ktor.http.Url

/**
 * Canonicalizes untrusted input into a bare absolute http(s) origin — `scheme://host:port` with the
 * port always explicit and the host lowercased — or returns null when it is anything else. Ktor's
 * `Url` is lenient — it "parses" free text against a localhost fallback and treats `localhost:8080`
 * as a scheme — so structural checks are needed on top of parse success. A path/query/fragment
 * suffix is rejected because [ApiConfig.baseUrl] must be origin-only (see its KDoc).
 */
fun normalizeOriginOrNull(url: String): String? {
    val candidate = url.trim()
    val schemeValid =
        candidate.startsWith("http://", ignoreCase = true) ||
            candidate.startsWith("https://", ignoreCase = true)
    if (!schemeValid || candidate.any { it.isWhitespace() }) return null
    val parsed = runCatching { Url(candidate) }.getOrNull() ?: return null
    if (parsed.host.isBlank()) return null
    if (parsed.encodedPath != "" && parsed.encodedPath != "/") return null
    if (parsed.encodedQuery.isNotEmpty() || parsed.encodedFragment.isNotEmpty()) return null
    return "${parsed.protocol.name}://${parsed.host.lowercase()}:${parsed.port}"
}
