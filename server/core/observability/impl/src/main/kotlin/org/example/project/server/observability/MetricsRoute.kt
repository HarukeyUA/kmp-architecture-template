package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.example.project.server.web.RouteRegistrar

/**
 * `GET /metrics` — Prometheus exposition scraped from the shared registry.
 *
 * Served only on the dedicated metrics connector ([MetricsConfig.port]). A request that arrives on
 * any other port (i.e. the public application port) gets a 404 — not a 403 — so the endpoint's
 * existence is never revealed on the public surface. Combined with keeping that port off the public
 * network, only an in-network Prometheus can reach it.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class MetricsRoute(
    private val registry: PrometheusMeterRegistry,
    private val config: MetricsConfig,
) : RouteRegistrar {
    override fun Application.register() {
        routing {
            get("/metrics") {
                if (call.request.local.localPort != config.port) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondText(registry.scrape())
            }
        }
    }
}
