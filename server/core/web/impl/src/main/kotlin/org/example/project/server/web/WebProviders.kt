package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.example.project.shared.common.buildSeamJson

@ContributesTo(AppScope::class)
interface WebProviders {
    /**
     * Per-domain `ApiError` [SerializersModule]s, each contributed by a `:server:feature:*:impl`
     * via `@ContributesIntoSet`. Empty until the first domain ships errors (ADR-0005).
     */
    @Multibinds(allowEmpty = true) fun serializersModules(): Set<SerializersModule>

    /**
     * Per-domain status mappings for domain-specific [org.example.project.shared.common.ApiError]s.
     */
    @Multibinds(allowEmpty = true) fun apiErrorStatusMappers(): Set<ApiErrorStatusMapper>

    /**
     * The single server-side [Json] — the multibound domain modules folded onto the cross-cutting
     * base via [buildSeamJson], the same builder the client uses, so the wire format matches
     * exactly.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(serializersModules: Set<SerializersModule>): Json =
        buildSeamJson(serializersModules)
}
