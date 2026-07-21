package org.example.project.core.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.serialization.json.Json
import org.example.project.shared.common.seamJson

@ContributesTo(AppScope::class)
interface NetworkProviders {
    /**
     * The client [Json] (used by the HttpClient's `ContentNegotiation`) — the static [seamJson] the
     * server uses too, so the wire format matches exactly. Errors ride sealed lenses decoded at the
     * call site (`toCallFailure`), so no per-domain `SerializersModule` registration exists.
     */
    @Provides fun provideJson(): Json = seamJson
}
