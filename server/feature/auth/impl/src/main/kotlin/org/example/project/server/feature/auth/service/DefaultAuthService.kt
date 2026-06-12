package org.example.project.server.feature.auth.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.Principal
import org.example.project.server.auth.Session
import org.example.project.server.auth.SessionStore
import org.example.project.server.feature.auth.Account
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.auth.PasswordHasher
import org.example.project.server.feature.auth.data.AccountRepository
import org.example.project.shared.auth.Email
import org.example.project.shared.auth.Password
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.Validation

@Inject
@ContributesBinding(AppScope::class)
class DefaultAuthService(
    private val repo: AccountRepository,
    private val hasher: PasswordHasher,
    private val sessionStore: SessionStore,
) : AuthService {

    override suspend fun signup(email: String, password: String): Either<ApiError, Session> =
        either {
            val validEmail = validateCredentials(email, password).bind()
            val passwordHash = hasher.hash(password)
            val account = repo.create(validEmail.value, passwordHash).bind()
            sessionStore.issue(account.id)
        }

    override suspend fun login(email: String, password: String): Either<ApiError, Session> =
        either {
            // Any credential problem collapses to Unauthorized — the information-disclosure
            // boundary (no distinguishing unknown-user from wrong-password).
            val validEmail = Email.of(email).getOrNull()?.value ?: raise(Unauthorized)
            val credential = repo.findCredentialByEmail(validEmail) ?: raise(Unauthorized)
            ensure(hasher.verify(password, credential.passwordHash)) { Unauthorized }
            sessionStore.issue(credential.accountId)
        }

    override suspend fun logout(token: String): Either<ApiError, Unit> = either {
        sessionStore.revoke(token)
    }

    override suspend fun me(principal: Principal): Either<ApiError, Account> = either {
        repo.findById(principal.accountId) ?: raise(Unauthorized)
    }

    /**
     * Shape-validates both fields and accumulates the failures into one [Validation] (ADR-0004).
     */
    private fun validateCredentials(email: String, password: String): Either<ApiError, Email> =
        Either.zipOrAccumulate(Email.of(email), Password.of(password)) { validEmail, _ ->
                validEmail
            }
            .mapLeft { errors -> Validation(errors.toList()) }
}
