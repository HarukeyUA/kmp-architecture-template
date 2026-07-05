package org.example.project.shared.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.example.project.shared.common.ApiError

/**
 * The auth domain's per-operation Declared-error lenses (ADR-0011): one sealed interface per
 * operation that commits to returning something, named `<Domain><Operation>Error`. Each is the
 * exact set the client handles exhaustively for that call; variants may implement several lenses if
 * shared. Cross-cutting failures never appear here — they ride every operation Ambiently.
 *
 * The information-disclosure boundary survives (ADR-0005): [InvalidCredentials] is a single
 * collapsed variant (unknown-user vs wrong-password stay indistinguishable, the dummy-verify timing
 * defense unaffected), and [SessionExpired] likewise. `refresh` presents a Session, not an Access
 * token, so it declares its own credential failure rather than reusing the cross-cutting
 * `Unauthorized`, which now means exactly "Access token missing/expired/invalid."
 */
sealed interface AuthSignupError : ApiError

sealed interface AuthLoginError : ApiError

sealed interface AuthRefreshError : ApiError

@Serializable @SerialName("auth.email_taken") data object EmailTaken : AuthSignupError

@Serializable
@SerialName("auth.invalid_credentials")
data object InvalidCredentials : AuthLoginError

@Serializable @SerialName("auth.session_expired") data object SessionExpired : AuthRefreshError

/**
 * The auth domain's contribution to the multibound `Set<SerializersModule>`. Each side's `:impl`
 * contributes this via Metro `@ContributesIntoSet`, and `buildSeamJson` folds it onto the base so
 * the auth variants round-trip across the seam. Registration is against polymorphic [ApiError]
 * regardless of which lens a variant implements (ADR-0011 leaves serialization untouched).
 */
val authErrorSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiError::class) {
        subclass(EmailTaken::class)
        subclass(InvalidCredentials::class)
        subclass(SessionExpired::class)
    }
}
