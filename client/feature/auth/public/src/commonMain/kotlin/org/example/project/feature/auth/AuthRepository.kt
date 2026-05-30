package org.example.project.feature.auth

import arrow.core.Either
import org.example.project.core.error.AppError

/**
 * Client-side auth against the server's `:shared:auth` contract. On success, the issued opaque
 * Session token is persisted in platform-secure storage — which flips the observed logged-in state
 * (and so drives navigation). Failures arrive as the existing typed [AppError] pipeline (a 4xx
 * `ErrorEnvelope` becomes `NetworkError.Api(ApiError)`).
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Either<AppError, Unit>

    suspend fun signup(email: String, password: String): Either<AppError, Unit>

    /** Best-effort server-side revoke, then clears the local session unconditionally. */
    suspend fun logout()
}
