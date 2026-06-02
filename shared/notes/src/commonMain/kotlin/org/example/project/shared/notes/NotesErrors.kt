package org.example.project.shared.notes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.example.project.shared.common.ApiError

/**
 * Notes domain errors. [NotesQuotaExceeded] is a genuine domain rule (not a cross-cutting variant)
 * — the per-account character budget — so it carries its own `notes.*` code and rides the wire
 * inside `ErrorEnvelope`, exercising the per-domain error path end-to-end for a second domain.
 */
@Serializable
@SerialName("notes.quota_exceeded")
data class NotesQuotaExceeded(
    @SerialName("quota") val quota: Int,
    @SerialName("used") val used: Int,
) : ApiError

/**
 * The notes domain's contribution to the multibound `Set<SerializersModule>`. Each side's `:impl`
 * folds this in via Metro `@Provides @IntoSet`, and `buildSeamJson` composes it onto the base so
 * [NotesQuotaExceeded] round-trips across the seam.
 */
val notesErrorSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiError::class) { subclass(NotesQuotaExceeded::class) }
}
