package org.example.project.server.auth

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext

/** Name of the JWT [io.ktor.server.auth.Authentication] provider installed by `:impl`. */
const val ACCESS_AUTH: String = "access-jwt"

/**
 * Wraps [block] so its routes require a valid [AccessToken]; unauthenticated requests get a 401.
 * Verification is stateless (signature + issuer/audience/expiry) — no session-store lookup.
 */
fun Route.authenticatedRoutes(block: Route.() -> Unit) = authenticate(ACCESS_AUTH, build = block)

/** The authenticated [Principal] — non-null inside [authenticatedRoutes]. */
fun RoutingContext.principal(): Principal =
    call.principal<Principal>()
        ?: error("No Principal on the call — route is not inside authenticatedRoutes { }")
