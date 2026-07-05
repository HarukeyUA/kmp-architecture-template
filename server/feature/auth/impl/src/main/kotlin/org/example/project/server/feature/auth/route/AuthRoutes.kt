package org.example.project.server.feature.auth.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.routing
import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.feature.auth.Account
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.auth.AuthTokens
import org.example.project.server.web.CREDENTIAL_RATE_LIMIT_NAME
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.serve
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.TokensResponse

/**
 * Self-registering auth routes (ADR-0006). The route is dumb and owns the Wire boundary (ADR-0003):
 * each [serve] binds a typed [AuthApi] endpoint, unpacks the request DTO into the service's
 * parameters, maps the returned domain model to the response DTO, and folds `Either<ApiError, T>`
 * to HTTP. `logout`/`me` sit under [authenticatedRoutes], so the JWT middleware 401s before the
 * handler runs.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AuthRoutes(private val service: AuthService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            // The pre-auth, Argon2-expensive surface opts into the strict per-IP tier registered
            // by the web core's rate-limit installer (ADR-0010 §11).
            rateLimit(RateLimitName(CREDENTIAL_RATE_LIMIT_NAME)) {
                serve(AuthApi.signup, HttpStatusCode.Created) { _, body ->
                    service.signup(body.email, body.password).map { it.toResponse() }
                }
                serve(AuthApi.login, HttpStatusCode.OK) { _, body ->
                    service.login(body.email, body.password).map { it.toResponse() }
                }
            }
            // Pre-auth like login, but outside the strict tier: it costs one cached indexed lookup
            // (no Argon2), and guessing a 256-bit opaque token by volume isn't a real threat.
            serve(AuthApi.refresh, HttpStatusCode.OK) { _, body ->
                service.refresh(body.refreshToken).map { it.toResponse() }
            }
            authenticatedRoutes {
                serve(AuthApi.logout, HttpStatusCode.NoContent) { _, body ->
                    service.logout(body.refreshToken)
                }
                serve(AuthApi.me, HttpStatusCode.OK) { _ ->
                    service.me(principal()).map { it.toResponse() }
                }
            }
        }
    }
}

private fun AuthTokens.toResponse(): TokensResponse =
    TokensResponse(
        accessToken = accessToken.token,
        accessTokenExpiresAt = accessToken.expiresAt,
        refreshToken = session.token,
        refreshTokenExpiresAt = session.expiresAt,
    )

private fun AccessToken.toResponse(): AccessTokenResponse =
    AccessTokenResponse(accessToken = token, expiresAt = expiresAt)

private fun Account.toResponse(): AccountResponse =
    AccountResponse(id = id.value.toString(), email = email)
