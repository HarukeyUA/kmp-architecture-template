package org.example.project.server.feature.auth

import arrow.core.Either
import org.example.project.server.auth.Principal
import org.example.project.server.auth.Session
import org.example.project.shared.common.ApiError

/**
 * The Credential module (ADR-0009): verifies an email+password credential and issues a Session via
 * the invariant [org.example.project.server.auth.SessionStore]. Swap this module to change *how you
 * log in* without touching session infra.
 *
 * Speaks **domain types** ([Session], [Account]) on the success channel and the shared `ApiError`
 * taxonomy on the error channel (ADR-0003 as amended; ADR-0005): the route owns the Wire boundary,
 * unpacking request DTOs into the parameters here and mapping the returned domain models to
 * response DTOs — so internal cross-domain callers never touch wire compat concerns.
 */
interface AuthService {
    suspend fun signup(email: String, password: String): Either<ApiError, Session>

    suspend fun login(email: String, password: String): Either<ApiError, Session>

    suspend fun logout(token: String): Either<ApiError, Unit>

    /** Resolves the [Principal]'s account; an account deleted mid-request is `Unauthorized`. */
    suspend fun me(principal: Principal): Either<ApiError, Account>
}
