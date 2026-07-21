package org.example.project.feature.auth.data

import arrow.core.Either
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import org.example.project.core.error.CallFailure
import org.example.project.core.network.call
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.AuthSignupError
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.LogoutRequest
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.TokensResponse

@Inject
@ContributesBinding(AppScope::class)
class AuthRepositoryImpl(
    private val client: HttpClient,
    private val sessionStore: SecureSessionStore,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): Either<CallFailure<AuthLoginError>, Unit> = either {
        val tokens =
            client
                .call(AuthApi.login, AuthResource.Login(), LoginRequest(email.trim(), password))
                .bind()
        sessionStore.save(tokens.toSession())
    }

    override suspend fun signup(
        email: String,
        password: String,
    ): Either<CallFailure<AuthSignupError>, Unit> = either {
        val tokens =
            client
                .call(AuthApi.signup, AuthResource.Signup(), SignupRequest(email.trim(), password))
                .bind()
        sessionStore.save(tokens.toSession())
    }

    override suspend fun logout() {
        // Best-effort server revoke of the refresh token (the access token rides along as the
        // bearer); `call` folds any network failure into the Either, so the local session is
        // cleared regardless.
        sessionStore.current()?.let { session ->
            client.call(AuthApi.logout, AuthResource.Logout(), LogoutRequest(session.refreshToken))
        }
        sessionStore.clear()
    }
}

private fun TokensResponse.toSession(): ClientSession =
    ClientSession(accessToken = accessToken, refreshToken = refreshToken)
