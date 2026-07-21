package org.example.project.server.feature.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import org.example.project.server.database.TableSet
import org.example.project.server.feature.auth.data.Accounts

/**
 * The auth domain's self-registrations. Adding a domain like this touches **zero lines** in
 * `:server:app` — the multibound sets assemble via Metro contribution merging (ADR-0006, ADR-0008).
 * Error serialization and statuses need no registration: both ride the sealed lenses and the
 * `ApiError.status` declarations in `:shared:auth`.
 */
@ContributesTo(AppScope::class)
interface AuthFeatureBindings {
    /** Joins the accounts table into the drift-tested `Set<TableSet>`. */
    @Provides @IntoSet fun accountsTableSet(): TableSet = TableSet(Accounts)
}
