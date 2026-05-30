package org.example.project.shared.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.example.project.shared.common.ApiError

/**
 * Auth domain errors. Per the interim stop-gap (ADR-0005) they are declared directly as `:
 * ApiError` with no sealed grouping. Login failures deliberately do **not** appear here —
 * unknown-user and wrong-password both collapse to the cross-cutting `Unauthorized` (the
 * information-disclosure boundary).
 */
@Serializable @SerialName("auth.email_taken") data object EmailTaken : ApiError

/**
 * The auth domain's contribution to the multibound `Set<SerializersModule>`. Each side's `:impl`
 * contributes this via Metro `@ContributesIntoSet`, and `buildSeamJson` folds it onto the base so
 * `EmailTaken` round-trips across the seam (ADR-0005).
 */
val authErrorSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiError::class) { subclass(EmailTaken::class) }
}
