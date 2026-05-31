package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.core.secure.storage.SecureSessionStore

internal expect fun httpClientEngine(): HttpClientEngineFactory<*>

@ContributesTo(AppScope::class)
interface HttpClientGraph {
    /**
     * The app's single [HttpClient]. ContentNegotiation uses the seam [Json] (so a 4xx
     * `ErrorEnvelope` parses into the typed `ApiError`); typed `@Resource` requests share the
     * seam's route definitions; the bearer token is read from [SecureSessionStore] on each request.
     *
     * The opaque Session has no refresh — so a 401 on an authenticated request means the session
     * was revoked or expired server-side. The [Auth] `refreshTokens` hook is the global "401 →
     * clear session" interceptor (ADR-0009): clearing makes [SecureSessionStore.sessionFlow] emit
     * null, which the root navigation observes and bounces to Login.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(
        seamJson: Json,
        apiConfig: ApiConfig,
        sessionStore: SecureSessionStore,
    ): HttpClient =
        HttpClient(httpClientEngine()) {
            install(ContentNegotiation) { json(seamJson) }
            install(Resources)
            install(Auth) {
                bearer {
                    cacheTokens = false
                    sendWithoutRequest { true }
                    loadTokens {
                        sessionStore.current()?.let { BearerTokens(it.token, refreshToken = null) }
                    }
                    refreshTokens {
                        sessionStore.clear()
                        null
                    }
                }
            }
            defaultRequest {
                url(apiConfig.baseUrl)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
}
