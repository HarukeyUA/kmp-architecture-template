package org.example.project.server.feature.auth.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.AccessTokenIssuer
import org.example.project.server.auth.AccountId
import org.example.project.server.auth.Principal
import org.example.project.server.auth.SessionStore
import org.example.project.server.feature.auth.Account
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.auth.AuthTokens
import org.example.project.server.feature.auth.PasswordHasher
import org.example.project.server.feature.auth.data.AccountRepository
import org.example.project.server.web.Failure
import org.example.project.server.web.ambient
import org.example.project.server.web.declared
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthRefreshError
import org.example.project.shared.auth.AuthSignupError
import org.example.project.shared.auth.Email
import org.example.project.shared.auth.InvalidCredentials
import org.example.project.shared.auth.Password
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.Validation

@Inject
@ContributesBinding(AppScope::class)
class DefaultAuthService(
    private val repo: AccountRepository,
    private val hasher: PasswordHasher,
    private val sessionStore: SessionStore,
    private val accessTokenIssuer: AccessTokenIssuer,
) : AuthService {

    override suspend fun signup(
        email: String,
        password: String,
    ): Either<Failure<AuthSignupError>, AuthTokens> = either {
        val validEmail = validateCredentials(email, password).getOrElse { ambient(it) }
        val passwordHash = hasher.hash(password)
        val account = repo.create(validEmail.value, passwordHash).getOrElse { declared(it) }
        issueTokens(account.id)
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Either<Failure<AuthLoginError>, AuthTokens> = either {
        // Any credential problem collapses to the single Declared InvalidCredentials — the
        // information-disclosure boundary (no distinguishing unknown-user from wrong-password). The
        // boundary covers timing too: the unknown-email path burns a dummy verify so both failures
        // cost one Argon2 verify. The malformed-email fast-fail below is deliberately exempt from
        // the dummy verify — it reveals only that the input isn't an email, which the caller
        // already knows — but still collapses to the same wire error.
        val validEmail = Email.of(email).getOrNull()?.value ?: declared(InvalidCredentials)
        val credential =
            repo.findCredentialByEmail(validEmail)
                ?: run {
                    hasher.verifyDummy(password)
                    declared(InvalidCredentials)
                }
        if (!hasher.verify(password, credential.passwordHash)) declared(InvalidCredentials)
        issueTokens(credential.accountId)
    }

    override suspend fun refresh(
        refreshToken: String
    ): Either<Failure<AuthRefreshError>, AccessToken> = either {
        // The only place a request touches the session store (cache → DB): a revoked or expired
        // session stops minting here, which bounds access-token staleness by its TTL. refresh
        // presents a Session, not an Access token, so its credential failure is the Declared
        // SessionExpired, never the cross-cutting Unauthorized (ADR-0011).
        val principal = sessionStore.resolve(refreshToken) ?: declared(SessionExpired)
        accessTokenIssuer.issue(principal.accountId)
    }

    override suspend fun logout(refreshToken: String): Either<Failure<Nothing>, Unit> = either {
        sessionStore.revoke(refreshToken)
    }

    override suspend fun me(principal: Principal): Either<Failure<Nothing>, Account> = either {
        repo.findById(principal.accountId) ?: ambient(Unauthorized)
    }

    private suspend fun issueTokens(accountId: AccountId): AuthTokens =
        AuthTokens(
            accessToken = accessTokenIssuer.issue(accountId),
            session = sessionStore.issue(accountId),
        )

    /**
     * Shape-validates both fields and accumulates the failures into one [Validation] (ADR-0004).
     */
    private fun validateCredentials(email: String, password: String): Either<ApiError, Email> =
        Either.zipOrAccumulate(Email.of(email), Password.of(password)) { validEmail, _ ->
                validEmail
            }
            .mapLeft { errors -> Validation(errors.toList()) }
}
