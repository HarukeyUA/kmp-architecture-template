package org.example.project.server.feature.auth

import arrow.core.Either
import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.Principal
import org.example.project.shared.common.ApiError

/**
 * The Credential module (ADR-0009, as amended): verifies an email+password credential and issues
 * [AuthTokens] — a JWT access token plus an opaque Session — via the invariant
 * [org.example.project.server.auth.SessionStore] /
 * [org.example.project.server.auth.AccessTokenIssuer]. Swap this module to change *how you log in*
 * without touching token infra.
 *
 * Speaks **domain types** ([AuthTokens], [Account]) on the success channel and the shared
 * `ApiError` taxonomy on the error channel (ADR-0003 as amended; ADR-0005): the route owns the Wire
 * boundary, unpacking request DTOs into the parameters here and mapping the returned domain models
 * to response DTOs — so internal cross-domain callers never touch wire compat concerns.
 */
interface AuthService {
    suspend fun signup(email: String, password: String): Either<ApiError, AuthTokens>

    suspend fun login(email: String, password: String): Either<ApiError, AuthTokens>

    /**
     * Mints a fresh [AccessToken] for a live session; a missing, expired, or revoked [refreshToken]
     * is `Unauthorized`. The session itself is untouched — no rotation (ADR-0009).
     */
    suspend fun refresh(refreshToken: String): Either<ApiError, AccessToken>

    /** Revokes the session behind [refreshToken]; already-minted access tokens expire on TTL. */
    suspend fun logout(refreshToken: String): Either<ApiError, Unit>

    /** Resolves the [Principal]'s account; an account deleted mid-request is `Unauthorized`. */
    suspend fun me(principal: Principal): Either<ApiError, Account>
}
