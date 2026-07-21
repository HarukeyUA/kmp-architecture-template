package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.example.project.shared.common.buildSeamJson

@ContributesTo(AppScope::class)
interface NetworkProviders {
    /**
     * Per-domain `ApiError` [SerializersModule]s, each contributed by a `:client:feature:*:impl`
     * via `@ContributesIntoSet` — the mirror of the server's set. Empty until a domain ships
     * errors.
     */
    @Multibinds(allowEmpty = true) fun serializersModules(): Set<SerializersModule>

    /**
     * The client [Json] (used by the HttpClient's `ContentNegotiation`), built with [buildSeamJson]
     * — the exact same builder + base module the server uses — so a 4xx `ErrorEnvelope` parses back
     * into the typed `ApiError`, with unknown codes degrading to `UnknownApiError` (ADR-0005).
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(serializersModules: Set<SerializersModule>): Json =
        buildSeamJson(serializersModules)
}
