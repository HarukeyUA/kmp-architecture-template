package org.example.project.server.feature.auth.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.auth.sessionToken
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.respondEither
import org.example.project.shared.auth.AuthResource

/**
 * Self-registering auth routes (ADR-0006). The route is dumb: receive the DTO, call the service,
 * and fold its `Either<ApiError, T>` to HTTP via [respondEither]. `logout`/`me` sit under
 * [authenticatedRoutes], so the session middleware 401s before the handler runs.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class AuthRoutes(private val service: AuthService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            post<AuthResource.Signup> {
                call.respondEither(service.signup(call.receive())) {
                    respond(HttpStatusCode.Created, it)
                }
            }
            post<AuthResource.Login> {
                call.respondEither(service.login(call.receive())) { respond(HttpStatusCode.OK, it) }
            }
            authenticatedRoutes {
                post<AuthResource.Logout> {
                    call.respondEither(service.logout(call.sessionToken().orEmpty())) {
                        respond(HttpStatusCode.NoContent)
                    }
                }
                get<AuthResource.Me> {
                    call.respondEither(service.me(principal())) { respond(HttpStatusCode.OK, it) }
                }
            }
        }
    }
}
