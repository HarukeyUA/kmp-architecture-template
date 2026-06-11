package org.example.project.server.feature.auth.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.auth.sessionToken
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.web.CREDENTIAL_RATE_LIMIT_NAME
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.serve
import org.example.project.shared.auth.AuthApi

/**
 * Self-registering auth routes (ADR-0006). The route is dumb: each [serve] binds a typed [AuthApi]
 * endpoint — body and response are checked against the same contract the client calls — then folds
 * the service's `Either<ApiError, T>` to HTTP. `logout`/`me` sit under [authenticatedRoutes], so
 * the session middleware 401s before the handler runs.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AuthRoutes(private val service: AuthService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            // The pre-auth, Argon2-expensive surface opts into the strict per-IP tier registered
            // by the web core's rate-limit installer (ADR-0010 §11).
            rateLimit(RateLimitName(CREDENTIAL_RATE_LIMIT_NAME)) {
                serve(AuthApi.signup, HttpStatusCode.Created) { _, body -> service.signup(body) }
                serve(AuthApi.login, HttpStatusCode.OK) { _, body -> service.login(body) }
            }
            authenticatedRoutes {
                serve(AuthApi.logout, HttpStatusCode.NoContent) { _ ->
                    service.logout(call.sessionToken().orEmpty())
                }
                serve(AuthApi.me, HttpStatusCode.OK) { _ -> service.me(principal()) }
            }
        }
    }
}
