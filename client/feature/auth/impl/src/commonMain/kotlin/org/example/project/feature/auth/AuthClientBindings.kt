package org.example.project.feature.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.serialization.modules.SerializersModule
import org.example.project.shared.auth.authErrorSerializersModule

/**
 * Registers the auth `ApiError` serializers into the client's multibound `Set<SerializersModule>` —
 * the mirror of the server's [org.example.project.server.feature.auth.AuthFeatureBindings]. Without
 * it the runtime seam `Json` (built by `:client:core:network` from the multibinding) lacks the auth
 * variants, so a 4xx [org.example.project.shared.auth.EmailTaken] degrades to `UnknownApiError`
 * instead of deserializing to its typed form (ADR-0005). The MockEngine test wires the module by
 * hand, so only this binding closes the gap on the real DI path.
 */
@ContributesTo(AppScope::class)
interface AuthClientBindings {
    @Provides @IntoSet fun authErrorModule(): SerializersModule = authErrorSerializersModule
}
