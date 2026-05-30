package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.example.project.server.web.RouteRegistrar

/** `GET /health` — aggregates every contributed [HealthIndicator]; 200 when all pass, else 503. */
@Inject
@ContributesIntoSet(AppScope::class)
class HealthRoute(private val indicators: Set<HealthIndicator>) : RouteRegistrar {
    override fun Application.register() {
        routing {
            get("/health") {
                val results = indicators.map { it.check() }
                val healthy = results.all { it.healthy }
                val body = buildJsonObject {
                    put("status", if (healthy) "UP" else "DOWN")
                    putJsonArray("checks") {
                        results.forEach { result ->
                            addJsonObject {
                                put("name", result.name)
                                put("healthy", result.healthy)
                                result.detail?.let { put("detail", it) }
                            }
                        }
                    }
                }
                call.respond(
                    if (healthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                    body,
                )
            }
        }
    }
}
