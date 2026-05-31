package org.example.project.shared.notes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.example.project.shared.common.ApiError

/**
 * Notes domain errors. Per the interim stop-gap (ADR-0005) they are declared directly as `:
 * ApiError` with no sealed grouping. [NotesQuotaExceeded] is a genuine domain rule (not a
 * cross-cutting variant) — the per-account character budget — so it carries its own `notes.*` code
 * and rides the wire inside `ErrorEnvelope`, exercising the per-domain error path end-to-end for a
 * second domain.
 */
@Serializable
@SerialName("notes.quota_exceeded")
data class NotesQuotaExceeded(val quota: Int, val used: Int) : ApiError

/**
 * The notes domain's contribution to the multibound `Set<SerializersModule>`. Each side's `:impl`
 * folds this in via Metro `@Provides @IntoSet`, and `buildSeamJson` composes it onto the base so
 * [NotesQuotaExceeded] round-trips across the seam (ADR-0005). This is the first contribution to
 * the **client's** previously-empty `Set<SerializersModule>` — proving the multibinding composes
 * with a real second domain on both ends.
 */
val notesErrorSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiError::class) { subclass(NotesQuotaExceeded::class) }
}
