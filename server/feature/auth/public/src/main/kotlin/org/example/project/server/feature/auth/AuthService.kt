package org.example.project.server.feature.auth

import arrow.core.Either
import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.Principal
import org.example.project.server.web.Failure
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthRefreshError
import org.example.project.shared.auth.AuthSignupError

/**
 * The Credential module (ADR-0009, as amended): verifies an email+password credential and issues
 * [AuthTokens] — a JWT access token plus an opaque Session — via the invariant
 * [org.example.project.server.auth.SessionStore] /
 * [org.example.project.server.auth.AccessTokenIssuer]. Swap this module to change *how you log in*
 * without touching token infra.
 *
 * Speaks **domain types** ([AuthTokens], [Account]) on the success channel and the two-arm
 * [Failure] on the error channel (ADR-0003 as amended; ADR-0005, ADR-0011): each operation's
 * `Failure<Err>` names the Declared errors that operation commits to, while cross-cutting failures
 * ride Ambiently. The route owns the Wire boundary, unpacking request DTOs into the parameters here
 * and mapping the returned domain models to response DTOs — so internal cross-domain callers never
 * touch wire compat concerns.
 */
interface AuthService {
    suspend fun signup(
        email: String,
        password: String,
    ): Either<Failure<AuthSignupError>, AuthTokens>

    suspend fun login(email: String, password: String): Either<Failure<AuthLoginError>, AuthTokens>

    /**
     * Mints a fresh [AccessToken] for a live session; a missing, expired, or revoked [refreshToken]
     * declares `auth.session_expired`. `refresh` presents a Session, not an Access token, so it
     * declares its own credential failure rather than reusing the cross-cutting `Unauthorized`
     * (ADR-0011). The session itself is untouched — no rotation (ADR-0009).
     */
    suspend fun refresh(refreshToken: String): Either<Failure<AuthRefreshError>, AccessToken>

    /** Revokes the session behind [refreshToken]; already-minted access tokens expire on TTL. */
    suspend fun logout(refreshToken: String): Either<Failure<Nothing>, Unit>

    /**
     * Resolves the [Principal]'s account; an account deleted mid-request is Ambient `Unauthorized`.
     */
    suspend fun me(principal: Principal): Either<Failure<Nothing>, Account>
}
