package org.example.project.server.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import org.example.project.server.auth.data.Sessions
import org.example.project.server.database.TableSet

/**
 * Contributes the sessions table into the multibound `Set<TableSet>` (drift test + schema tooling).
 * Values join a multibinding via `@Provides @IntoSet` in a `@ContributesTo` interface — Metro's
 * `@ContributesIntoSet` only targets classes.
 */
@ContributesTo(AppScope::class)
interface AuthServerBindings {
    @Provides @IntoSet fun sessionsTableSet(): TableSet = TableSet(Sessions)
}
