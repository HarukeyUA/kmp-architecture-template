package org.example.project.feature.auth.data

import arrow.core.Either
import arrow.core.raise.either
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import org.example.project.core.error.CallFailure
import org.example.project.core.error.NetworkError
import org.example.project.core.network.call
import org.example.project.feature.auth.AuthRepository
import org.example.project.feature.session.ClientSession
import org.example.project.feature.session.SessionRepository
import org.example.project.feature.session.SessionRepositoryError
import org.example.project.feature.session.boundSessionOrNull
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
    private val sessions: SessionRepository,
) : AuthRepository {
    private val log = Logger.withTag("AuthRepository")

    override suspend fun login(
        email: String,
        password: String,
    ): Either<CallFailure<AuthLoginError>, Unit> = either {
        val tokens =
            client
                .call(AuthApi.login, AuthResource.Login(), LoginRequest(email.trim(), password))
                .bind()
        sessions.persist(tokens.toSession()).mapLeft { it.toCallFailure() }.bind()
    }

    override suspend fun signup(
        email: String,
        password: String,
    ): Either<CallFailure<AuthSignupError>, Unit> = either {
        val tokens =
            client
                .call(AuthApi.signup, AuthResource.Signup(), SignupRequest(email.trim(), password))
                .bind()
        sessions.persist(tokens.toSession()).mapLeft { it.toCallFailure() }.bind()
    }

    override suspend fun logout(): Either<CallFailure<Nothing>, Unit> {
        // Best-effort server revoke of the refresh token (the access token rides along as the
        // bearer); `call` folds any network failure into the Either, so the local session is
        // cleared regardless.
        sessions.boundSessionOrNull(log)?.let { session ->
            client.call(AuthApi.logout, AuthResource.Logout(), LogoutRequest(session.refreshToken))
        }
        return sessions
            .clear()
            .onLeft { log.e { "session not cleared on logout: $it" } }
            .mapLeft { it.toCallFailure() }
    }
}

// The account exists but the device could not keep its tokens: no typed wire error applies, so it
// rides the generic transport rendering and the user can sign in again.
private fun SessionRepositoryError.toCallFailure(): CallFailure.Transport =
    when (this) {
        is SessionRepositoryError.Storage ->
            CallFailure.Transport(NetworkError.Serialization(cause))
    }

private fun TokensResponse.toSession(): ClientSession =
    ClientSession(accessToken = accessToken, refreshToken = refreshToken)
