package org.example.project.server.feature.auth.service

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.sql.SQLException
import org.example.project.server.auth.Principal
import org.example.project.server.auth.Session
import org.example.project.server.auth.SessionStore
import org.example.project.server.database.dbTransaction
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.auth.PasswordHasher
import org.example.project.server.feature.auth.data.AccountRepository
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.Email
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.Password
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest
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

    override suspend fun signup(request: SignupRequest): Either<ApiError, SessionResponse> =
        either {
            val email = validateCredentials(request.email, request.password).bind()
            val passwordHash = hasher.hash(request.password)
            // The service owns the transaction; the uniqueness check and insert are atomic
            // (ADR-0006).
            val account =
                catch({
                    dbTransaction {
                        ensure(repo.findByEmail(email.value) == null) { EmailTaken }
                        repo.insert(email.value, passwordHash)
                    }
                }) { e: SQLException ->
                    ensure(e.sqlState != UNIQUE_VIOLATION) { EmailTaken }
                    throw e
                }
            sessionStore.issue(account.id).toSessionResponse()
        }

    override suspend fun login(request: LoginRequest): Either<ApiError, SessionResponse> = either {
        // Any credential problem collapses to Unauthorized — the information-disclosure boundary
        // (no distinguishing unknown-user from wrong-password).
        val email = Email.of(request.email).getOrNull()?.value ?: raise(Unauthorized)
        val account = dbTransaction { repo.findByEmail(email) } ?: raise(Unauthorized)
        ensure(hasher.verify(request.password, account.passwordHash)) { Unauthorized }
        sessionStore.issue(account.id).toSessionResponse()
    }

    override suspend fun logout(token: String): Either<ApiError, Unit> = either {
        sessionStore.revoke(token)
    }

    override suspend fun me(principal: Principal): Either<ApiError, AccountResponse> = either {
        val account = dbTransaction { repo.findById(principal.accountId) } ?: raise(Unauthorized)
        AccountResponse(id = account.id.value.toString(), email = account.email)
    }

    /**
     * Shape-validates both fields and accumulates the failures into one [Validation] (ADR-0004).
     */
    private fun validateCredentials(email: String, password: String): Either<ApiError, Email> =
        Either.zipOrAccumulate(Email.of(email), Password.of(password)) { validEmail, _ ->
                validEmail
            }
            .mapLeft { errors -> Validation(errors.toList()) }

    private fun Session.toSessionResponse(): SessionResponse =
        SessionResponse(token = token, expiresAt = expiresAt)

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
