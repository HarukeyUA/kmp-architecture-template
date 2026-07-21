package org.example.project.feature.auth

import arrow.core.Either
import arrow.core.right
import org.example.project.core.error.CallFailure
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthSignupError

/**
 * Controllable [AuthRepository] fake: [login]/[signup] return their configured results (each
 * carrying that operation's Declared errors); [logout] flips [loggedOut].
 */
class FakeAuthRepository(
    private val loginResult: Either<CallFailure<AuthLoginError>, Unit> = Unit.right(),
    private val signupResult: Either<CallFailure<AuthSignupError>, Unit> = Unit.right(),
) : AuthRepository {
    var loggedOut: Boolean = false
        private set

    override suspend fun login(
        email: String,
        password: String,
    ): Either<CallFailure<AuthLoginError>, Unit> = loginResult

    override suspend fun signup(
        email: String,
        password: String,
    ): Either<CallFailure<AuthSignupError>, Unit> = signupResult

    override suspend fun logout() {
        loggedOut = true
    }
}
