package org.example.project.feature.auth

import arrow.core.Either
import arrow.core.right
import org.example.project.core.error.AppError

/** Controllable [AuthRepository] fake: every call returns [result]; [logout] flips [loggedOut]. */
class FakeAuthRepository(private val result: Either<AppError, Unit> = Unit.right()) :
    AuthRepository {
    var loggedOut: Boolean = false
        private set

    override suspend fun login(email: String, password: String): Either<AppError, Unit> = result

    override suspend fun signup(email: String, password: String): Either<AppError, Unit> = result

    override suspend fun logout() {
        loggedOut = true
    }
}
