package org.example.project.server.feature.auth

import arrow.core.Either
import org.example.project.server.auth.Principal
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.common.ApiError

/**
 * The Credential module (ADR-0009): verifies an email+password credential and issues a Session via
 * the invariant [org.example.project.server.auth.SessionStore]. Returns `Either<ApiError, T>`; the
 * route maps it to HTTP. Swap this module to change *how you log in* without touching session
 * infra.
 */
interface AuthService {
    suspend fun signup(request: SignupRequest): Either<ApiError, SessionResponse>

    suspend fun login(request: LoginRequest): Either<ApiError, SessionResponse>

    suspend fun logout(token: String): Either<ApiError, Unit>

    suspend fun me(principal: Principal): Either<ApiError, AccountResponse>
}
