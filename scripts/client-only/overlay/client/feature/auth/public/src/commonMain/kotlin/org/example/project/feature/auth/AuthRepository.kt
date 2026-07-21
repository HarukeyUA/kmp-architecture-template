package org.example.project.feature.auth

import arrow.core.Either
import org.example.project.core.error.AppError

/**
 * Client-side auth. On success a session is persisted in platform-secure storage — which flips the
 * observed logged-in state (and so drives navigation). The template implementation is a local fake
 * that accepts any credentials; replace it with a repository backed by your API.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Either<AppError, Unit>

    suspend fun signup(email: String, password: String): Either<AppError, Unit>

    /** Clears the local session. */
    suspend fun logout()
}
