package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal expect fun httpClientEngine(): HttpClientEngineFactory<*>

@ContributesTo(AppScope::class)
interface HttpClientGraph {
    /**
     * The app's single [HttpClient]. Wire an auth plugin (e.g. Ktor's `Auth` with a bearer
     * provider backed by `SecureSessionStore`) here once the app talks to an authenticated API.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(json: Json, apiConfig: ApiConfig): HttpClient =
        HttpClient(httpClientEngine()) {
            install(ContentNegotiation) { json(json) }
            install(Resources)
            defaultRequest {
                url(apiConfig.baseUrl)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
}
