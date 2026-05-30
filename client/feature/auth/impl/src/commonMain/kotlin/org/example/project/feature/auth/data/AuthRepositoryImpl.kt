package org.example.project.feature.auth.data

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.core.error.AppError
import org.example.project.core.network.executeSafe
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.feature.auth.AuthRepository
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest

@Inject
@ContributesBinding(AppScope::class)
class AuthRepositoryImpl(
    private val client: HttpClient,
    private val sessionStore: SecureSessionStore,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Either<AppError, Unit> = either {
        val session =
            executeSafe({
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest(email.trim(), password))
                    }
                }) {
                    it.body<SessionResponse>()
                }
                .bind()
        sessionStore.save(ClientSession(session.token))
    }

    override suspend fun signup(email: String, password: String): Either<AppError, Unit> = either {
        val session =
            executeSafe({
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(SignupRequest(email.trim(), password))
                    }
                }) {
                    it.body<SessionResponse>()
                }
                .bind()
        sessionStore.save(ClientSession(session.token))
    }

    override suspend fun logout() {
        // Best-effort server revoke (the bearer token is attached automatically); the local session
        // is cleared regardless so the device is signed out even if the network call fails.
        catch({ client.post(AuthResource.Logout()) }) { /* ignore — clear locally below */ }
        sessionStore.clear()
    }
}
