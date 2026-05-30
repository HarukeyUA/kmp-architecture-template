package org.example.project.server.auth

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext

/** Name of the session [io.ktor.server.auth.Authentication] provider installed by `:impl`. */
const val SESSION_AUTH: String = "session"

private const val BEARER_PREFIX = "Bearer "

/** Wraps [block] so its routes require a valid Session; unauthenticated requests get a 401. */
fun Route.authenticatedRoutes(block: Route.() -> Unit) = authenticate(SESSION_AUTH, build = block)

/** The authenticated [Principal] — non-null inside [authenticatedRoutes]. */
fun RoutingContext.principal(): Principal =
    call.principal<Principal>()
        ?: error("No Principal on the call — route is not inside authenticatedRoutes { }")

/** The raw bearer token on this call, if present — used by logout to revoke the current session. */
fun ApplicationCall.sessionToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
        ?.substring(BEARER_PREFIX.length)
        ?.trim()
