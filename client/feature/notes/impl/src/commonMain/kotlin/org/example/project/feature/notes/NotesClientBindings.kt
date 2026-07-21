package org.example.project.feature.notes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.serialization.modules.SerializersModule
import org.example.project.shared.notes.notesErrorSerializersModule

/**
 * Registers the notes `ApiError` serializers into the client's multibound `Set<SerializersModule>`
 * — the mirror of the server's [org.example.project.server.feature.notes.NotesFeatureBindings].
 * This is the first contribution to that previously-empty set, so a 4xx
 * [org.example.project.shared.notes.NotesQuotaExceeded] now deserializes to the typed error on the
 * client instead of degrading to `UnknownApiError` (ADR-0005).
 */
@ContributesTo(AppScope::class)
interface NotesClientBindings {
    @Provides @IntoSet fun notesErrorModule(): SerializersModule = notesErrorSerializersModule
}
