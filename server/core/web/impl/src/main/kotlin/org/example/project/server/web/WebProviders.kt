package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.serialization.json.Json
import org.example.project.shared.common.seamJson

@ContributesTo(AppScope::class)
interface WebProviders {
    /**
     * The single server-side [Json] — the static [seamJson] the client uses too, so the wire format
     * matches exactly. Errors ride sealed lenses, so no per-domain `SerializersModule` registration
     * exists to fold in.
     */
    @Provides fun provideJson(): Json = seamJson
}
