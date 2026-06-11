package org.example.project.server.web

/**
 * App-layer DoS limits (ADR-0010 §11): platform L4 protection does not cover the application layer,
 * so the app caps what a request may cost before any handler runs.
 *
 * @property maxRequestBodyBytes Upper bound for any request body; over-limit requests answer with
 *   the typed `PayloadTooLarge` envelope (413). Blobs bypass the app via presigned URLs, so nothing
 *   legitimate approaches this.
 * @property clientIpHeader The trusted proxy header carrying the real client IP (Railway:
 *   `X-Real-IP`; Cloudflare-fronted: `CF-Connecting-IP`), or null when directly exposed. A header
 *   *name* rather than a trust-the-proxy boolean because which header is trustworthy is
 *   deployment-specific; a missing header falls back to the socket address rather than collapsing
 *   all clients into one shared bucket.
 * @property credentialRateLimit Requests per minute per client IP for the credential endpoints
 *   (signup/login — the pre-auth, Argon2-expensive surface). Per-node by design (ADR-0010); no
 *   default tier exists for other endpoints.
 */
data class WebLimitsConfig(
    val maxRequestBodyBytes: Long,
    val clientIpHeader: String?,
    val credentialRateLimit: Int,
)

/**
 * The named rate-limit tier for credential endpoints. Registered centrally by the web `:impl`'s
 * rate-limit installer; a Credential module opts its pre-auth routes in via Ktor's
 * `rateLimit(RateLimitName(CREDENTIAL_RATE_LIMIT_NAME)) { }`.
 */
const val CREDENTIAL_RATE_LIMIT_NAME: String = "credential-endpoints"
