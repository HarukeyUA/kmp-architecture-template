package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
interface WebProviders {
    /**
     * The single server-side [Json]. Forward-compatible by default (`ignoreUnknownKeys`,
     * `explicitNulls = false`) so old clients tolerate new fields.
     *
     * Phase 3 replaces this with a builder that folds the multibound `Set<SerializersModule>` from
     * every domain so polymorphic `ApiError`s serialize across the seam (ADR-0005).
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
}
