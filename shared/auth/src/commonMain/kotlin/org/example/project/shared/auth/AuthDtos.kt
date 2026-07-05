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
 * The issued token pair (ADR-0009 as amended): a short-lived JWT [accessToken] sent as the bearer
 * on every authenticated request, and the opaque [refreshToken] (the server-side Session) presented
 * only to `refresh`/`logout`. Both live in platform-secure storage; revocation is a server-side row
 * delete that cuts off refresh, surfacing as a 401 within the access token's TTL.
 */
@Serializable
data class TokensResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("accessTokenExpiresAt") val accessTokenExpiresAt: Instant,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("refreshTokenExpiresAt") val refreshTokenExpiresAt: Instant,
)

/** Presents the opaque [refreshToken] to mint a fresh access token. */
@Serializable data class RefreshRequest(@SerialName("refreshToken") val refreshToken: String)

/** A freshly minted access token; the refresh token that minted it stays unchanged. */
@Serializable
data class AccessTokenResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiresAt") val expiresAt: Instant,
)

/**
 * Names the [refreshToken] to revoke. Logout revokes server-side state (the Session); the bearer
 * access token on the request can't identify it, because access tokens are stateless.
 */
@Serializable data class LogoutRequest(@SerialName("refreshToken") val refreshToken: String)

/** The authenticated Principal's public account view, returned by `GET /v1/auth/me`. */
@Serializable
data class AccountResponse(@SerialName("id") val id: String, @SerialName("email") val email: String)
