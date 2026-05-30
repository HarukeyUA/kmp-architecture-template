package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

/**
 * Safety net for **unexpected** exceptions only: log the cause and return a generic 500, never
 * leaking internals. Expected failures travel as typed `Either<ApiError, T>` and are mapped by the
 * route layer (ADR-0005) — Phase 3 makes this responder emit a proper `ErrorEnvelope`.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class StatusPagesPluginInstaller : PluginInstaller {
    override val order: PluginOrder = PluginOrder.STATUS_PAGES

    override fun Application.install() {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
            }
        }
    }
}
