package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface NetworkProviders {
    /**
     * The client [Json] used by the HttpClient's `ContentNegotiation`. Forward-compatible by
     * default: unknown keys are ignored and absent optionals are omitted on the wire.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
