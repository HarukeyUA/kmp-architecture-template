package org.example.project.feature.auth

import arrow.core.Either
import arrow.core.right
import org.example.project.core.error.AppError

class FakeAuthRepository(
    private val loginResult: Either<AppError, Unit> = Unit.right(),
    private val signupResult: Either<AppError, Unit> = Unit.right(),
) : AuthRepository {
    var loggedOut: Boolean = false
        private set

    override suspend fun login(email: String, password: String): Either<AppError, Unit> =
        loginResult

    override suspend fun signup(email: String, password: String): Either<AppError, Unit> =
        signupResult

    override suspend fun logout() {
        loggedOut = true
    }
}
