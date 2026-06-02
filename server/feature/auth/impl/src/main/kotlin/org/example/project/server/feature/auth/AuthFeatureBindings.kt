package org.example.project.server.feature.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.modules.SerializersModule
import org.example.project.server.database.TableSet
import org.example.project.server.feature.auth.data.Accounts
import org.example.project.server.web.ApiErrorStatusMapper
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.authErrorSerializersModule

/**
 * The auth domain's self-registrations. Adding a domain like this touches **zero lines** in
 * `:server:app` — the multibound sets assemble via Metro contribution merging (ADR-0006, ADR-0008).
 */
@ContributesTo(AppScope::class)
interface AuthFeatureBindings {
    /** Joins the accounts table into the drift-tested `Set<TableSet>`. */
    @Provides @IntoSet fun accountsTableSet(): TableSet = TableSet(Accounts)

    /** Joins the auth `ApiError` serializers into the `Json`-building `Set<SerializersModule>`. */
    @Provides @IntoSet fun authErrorModule(): SerializersModule = authErrorSerializersModule

    /** `auth.email_taken` is a state conflict, not a generic bad request. */
    @Provides
    @IntoSet
    fun authErrorStatusMapper(): ApiErrorStatusMapper = ApiErrorStatusMapper { error ->
        when (error) {
            EmailTaken -> HttpStatusCode.Conflict
            else -> null
        }
    }
}
