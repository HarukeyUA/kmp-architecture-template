package org.example.project.server.feature.notes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.modules.SerializersModule
import org.example.project.server.database.TableSet
import org.example.project.server.feature.notes.data.Notes
import org.example.project.server.web.ApiErrorStatusMapper
import org.example.project.shared.notes.NotesQuotaExceeded
import org.example.project.shared.notes.notesErrorSerializersModule

/**
 * The notes domain's self-registrations. Adding this second domain touches **zero lines** in
 * `:server:app` — the multibound sets assemble via Metro contribution merging, exactly as the auth
 * slice does (ADR-0006, ADR-0008). The drift test picks up [Notes] and the seam `Json` picks up the
 * error module automatically.
 */
@ContributesTo(AppScope::class)
interface NotesFeatureBindings {
    /** Joins the notes table into the drift-tested `Set<TableSet>`. */
    @Provides @IntoSet fun notesTableSet(): TableSet = TableSet(Notes)

    /** Joins the notes `ApiError` serializers into the `Json`-building `Set<SerializersModule>`. */
    @Provides @IntoSet fun notesErrorModule(): SerializersModule = notesErrorSerializersModule

    /** `notes.quota_exceeded` is a conflict with the account's current quota state. */
    @Provides
    @IntoSet
    fun notesErrorStatusMapper(): ApiErrorStatusMapper = ApiErrorStatusMapper { error ->
        when (error) {
            is NotesQuotaExceeded -> HttpStatusCode.Conflict
            else -> null
        }
    }
}
