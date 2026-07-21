package org.example.project.server.feature.notes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import org.example.project.server.database.TableSet
import org.example.project.server.feature.notes.data.Notes

/**
 * The notes domain's self-registrations. Adding this second domain touches **zero lines** in
 * `:server:app` — the multibound sets assemble via Metro contribution merging, exactly as the auth
 * slice does (ADR-0006, ADR-0008). The drift test picks up [Notes] automatically; error
 * serialization and statuses need no registration — both ride the sealed lens and the
 * `ApiError.status` declaration in `:shared:notes`.
 */
@ContributesTo(AppScope::class)
interface NotesFeatureBindings {
    /** Joins the notes table into the drift-tested `Set<TableSet>`. */
    @Provides @IntoSet fun notesTableSet(): TableSet = TableSet(Notes)
}
