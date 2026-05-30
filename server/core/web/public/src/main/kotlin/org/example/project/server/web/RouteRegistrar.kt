package org.example.project.server.web

import io.ktor.server.application.Application

/**
 * A self-registering route group. Each domain's `:impl` contributes one (or more) via
 * `@ContributesIntoSet(AppScope::class)`; `:server:app` installs the whole `Set<RouteRegistrar>`
 * without a hand-maintained registry, so adding a domain touches zero lines in the app (ADR-0006,
 * ADR-0008).
 *
 * Implementations take their service/controller via constructor injection and open their own
 * `routing { }` (and `authenticate { }`) block inside [register].
 */
fun interface RouteRegistrar {
    fun Application.register()
}
