package org.example.project.shared.auth

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.shared.common.ApiError

/**
 * The auth domain's per-operation Declared-error lenses (ADR-0011): one `@Serializable sealed
 * interface` per operation that commits to returning something, named `<Domain><Operation>Error`.
 * Each is the exact set the client handles exhaustively for that call; variants may implement
 * several lenses if shared. Cross-cutting failures never appear here — they ride every operation
 * Ambiently. Statuses live on the variants themselves; codes and statuses are frozen by
 * `AuthDeclaredErrorFreezeTest`.
 *
 * The information-disclosure boundary survives (ADR-0005): [InvalidCredentials] is a single
 * collapsed variant (unknown-user vs wrong-password stay indistinguishable, the dummy-verify timing
 * defense unaffected), and [SessionExpired] likewise. `refresh` presents a Session, not an Access
 * token, so it declares its own credential failure rather than reusing the cross-cutting
 * `Unauthorized`, which now means exactly "Access token missing/expired/invalid."
 */
@Serializable sealed interface AuthSignupError : ApiError

@Serializable sealed interface AuthLoginError : ApiError

@Serializable sealed interface AuthRefreshError : ApiError

/** `auth.email_taken` is a state conflict, not a generic bad request. */
@Serializable
@SerialName("auth.email_taken")
data object EmailTaken : AuthSignupError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Conflict
}

/**
 * The Declared credential failures that took over login/refresh from the cross-cutting
 * `Unauthorized` (ADR-0011); both stay `401` so the wire status is unchanged and only the error
 * body's discriminator differs.
 */
@Serializable
@SerialName("auth.invalid_credentials")
data object InvalidCredentials : AuthLoginError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Unauthorized
}

@Serializable
@SerialName("auth.session_expired")
data object SessionExpired : AuthRefreshError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Unauthorized
}
