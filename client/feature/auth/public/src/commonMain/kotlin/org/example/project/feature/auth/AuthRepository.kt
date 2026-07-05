package org.example.project.feature.auth

import arrow.core.Either
import org.example.project.core.error.CallFailure
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError

/**
 * Client-side auth against the server's `:shared:auth` contract. On success, the issued token pair
 * (JWT access + opaque refresh) is persisted in platform-secure storage — which flips the observed
 * logged-in state (and so drives navigation). Failures arrive as a typed [CallFailure] carrying
 * each operation's Declared errors (ADR-0011): `login` may return [AuthLoginError], `signup`
 * [AuthSignupError]; the repository preserves that set unmapped for the feature to branch on.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Either<CallFailure<AuthLoginError>, Unit>

    suspend fun signup(email: String, password: String): Either<CallFailure<AuthSignupError>, Unit>

    /** Best-effort server-side refresh-token revoke, then clears the local session regardless. */
    suspend fun logout()
}
