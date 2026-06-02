package org.example.project.shared.auth

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Credentials for creating an account. Shape-validated on both sides via [Email] / [Password]. */
@Serializable
data class SignupRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

/** Credentials for logging in. */
@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

/**
 * The issued opaque Session: the bearer [token] and when it [expiresAt]. The token is the only
 * credential the client stores (in platform-secure storage); revocation is a server-side row
 * delete, surfaced to the client as a 401.
 */
@Serializable
data class SessionResponse(
    @SerialName("token") val token: String,
    @SerialName("expiresAt") val expiresAt: Instant,
)

/** The authenticated Principal's public account view, returned by `GET /v1/auth/me`. */
@Serializable
data class AccountResponse(@SerialName("id") val id: String, @SerialName("email") val email: String)
