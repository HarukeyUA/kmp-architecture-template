package org.example.project.feature.auth.data

import arrow.core.Either
import arrow.core.right
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.core.error.AppError
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.feature.auth.AuthRepository

/**
 * Local fake: accepts any credentials and stores a stub session in secure storage, so the
 * logged-in flow (splash routing, logout, session observation) is exercised end to end without a
 * server. Replace with a repository backed by your API.
 */
@Inject
@ContributesBinding(AppScope::class)
class AuthRepositoryImpl(private val sessionStore: SecureSessionStore) : AuthRepository {
    override suspend fun login(email: String, password: String): Either<AppError, Unit> =
        startSession()

    override suspend fun signup(email: String, password: String): Either<AppError, Unit> =
        startSession()

    override suspend fun logout() {
        sessionStore.clear()
    }

    private suspend fun startSession(): Either<AppError, Unit> {
        sessionStore.save(ClientSession(accessToken = "fake-access", refreshToken = "fake-refresh"))
        return Unit.right()
    }
}
