package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.auth.Auth
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
     * seam's route definitions; [sessionBearer] attaches the stored access token, refreshes it
     * through the refresh endpoint on 401, and clears the session (→ Login) when the refresh itself
     * is rejected (ADR-0009 as amended).
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
            install(Auth) { sessionBearer(sessionStore) }
            defaultRequest {
                url(apiConfig.baseUrl)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
}
