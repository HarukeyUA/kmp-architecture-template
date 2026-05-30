package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.example.project.server.web.RouteRegistrar

/** `GET /metrics` — Prometheus exposition scraped from the shared registry. */
@Inject
@ContributesIntoSet(AppScope::class)
class MetricsRoute(private val registry: PrometheusMeterRegistry) : RouteRegistrar {
    override fun Application.register() {
        routing { get("/metrics") { call.respondText(registry.scrape()) } }
    }
}
