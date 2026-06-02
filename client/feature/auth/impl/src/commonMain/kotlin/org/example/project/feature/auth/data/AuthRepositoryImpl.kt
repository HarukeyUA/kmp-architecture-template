package org.example.project.feature.auth.data

import arrow.core.Either
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import org.example.project.core.error.AppError
import org.example.project.core.network.call
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SignupRequest

@Inject
@ContributesBinding(AppScope::class)
class AuthRepositoryImpl(
    private val client: HttpClient,
    private val sessionStore: SecureSessionStore,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Either<AppError, Unit> = either {
        val session =
            client
                .call(AuthApi.login, AuthResource.Login(), LoginRequest(email.trim(), password))
                .bind()
        sessionStore.save(ClientSession(session.token))
    }

    override suspend fun signup(email: String, password: String): Either<AppError, Unit> = either {
        val session =
            client
                .call(AuthApi.signup, AuthResource.Signup(), SignupRequest(email.trim(), password))
                .bind()
        sessionStore.save(ClientSession(session.token))
    }

    override suspend fun logout() {
        // Best-effort server revoke (the bearer token is attached automatically); `call` folds any
        // network failure into the Either, so the local session is cleared regardless.
        client.call(AuthApi.logout, AuthResource.Logout())
        sessionStore.clear()
    }
}
