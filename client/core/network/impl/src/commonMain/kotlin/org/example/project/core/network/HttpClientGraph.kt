package org.example.project.core.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.core.buildinfo.Environment
import org.example.project.core.secure.storage.SecureSessionStore

internal expect fun httpClientEngine(): HttpClientEngineFactory<*>

// PaaS deployments (Railway et al.) sleep the server after periods of inactivity; the first
// request after a wake-up can spend 20–30s on cold-start before any response bytes arrive. These
// timeouts are sized to outlast that window so users don't see spurious failures on the first
// call of a session.
private const val REQUEST_TIMEOUT_MS = 60_000L
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val SOCKET_TIMEOUT_MS = 60_000L

@ContributesTo(AppScope::class)
interface HttpClientGraph {
    /**
     * The app's single [HttpClient]. ContentNegotiation uses the seam [Json] (so a 4xx
     * `ErrorEnvelope` parses into the typed `ApiError`); typed `@Resource` requests share the
     * seam's route definitions; [sessionBearer] attaches the stored access token, refreshes it
     * through the refresh endpoint on 401, and clears the session (→ Login) when the refresh itself
     * is rejected (ADR-0009 as amended).
     *
     * Wire logging rides Kermit under the `ktor` tag and exists only in [Environment.DEV] — the
     * plugin is never installed in PROD, so no request/response detail can reach a release log. The
     * Authorization header is redacted even in DEV: a bearer pasted with a log excerpt is a session
     * hijack.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(
        seamJson: Json,
        apiConfig: ApiConfig,
        sessionStore: SecureSessionStore,
        environment: Environment,
    ): HttpClient =
        HttpClient(httpClientEngine()) {
            install(ContentNegotiation) { json(seamJson) }
            install(Resources)
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
            if (environment == Environment.DEV) {
                val log = Logger.withTag("ktor")
                install(Logging) {
                    level = LogLevel.ALL
                    sanitizeHeader { it == HttpHeaders.Authorization }
                    logger =
                        object : KtorLogger {
                            override fun log(message: String) {
                                log.i { message }
                            }
                        }
                }
            }
            install(Auth) { sessionBearer(sessionStore) }
            defaultRequest {
                url(apiConfig.baseUrl)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
}
