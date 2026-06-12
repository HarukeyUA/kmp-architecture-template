package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.server.web.RouteRegistrar
import org.slf4j.LoggerFactory

/**
 * `GET /health` — aggregates every contributed [HealthIndicator]; 200 when all pass, else 503.
 *
 * The body is **status-only** (`{"status":"UP"|"DOWN"}`) on every connector: a probe needs nothing
 * but the status code, and per-check names/details on the public port would hand any prober the
 * infra composition. Diagnostics go where operators already look (§10 structured logging) — each
 * failing check is WARN-logged with its name and detail, so an outage under a periodic prober
 * produces a log heartbeat naming the culprit.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class HealthRoute(private val indicators: Set<HealthIndicator>) : RouteRegistrar {
    private val logger = LoggerFactory.getLogger(HealthRoute::class.java)

    override fun Application.register() {
        routing {
            get("/health") {
                val results = indicators.map { it.check() }
                results
                    .filterNot { it.healthy }
                    .forEach { result ->
                        logger.warn(
                            "Health check '{}' failing{}",
                            result.name,
                            result.detail?.let { ": $it" }.orEmpty(),
                        )
                    }
                val healthy = results.all { it.healthy }
                call.respond(
                    if (healthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                    buildJsonObject { put("status", if (healthy) "UP" else "DOWN") },
                )
            }
        }
    }
}
