package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/** Feeds Ktor request metrics into the shared Prometheus registry exposed by `/metrics`. */
@Inject
@ContributesIntoSet(AppScope::class)
class MicrometerMetricsPluginInstaller(private val meterRegistry: PrometheusMeterRegistry) :
    PluginInstaller {
    override val order: PluginOrder = PluginOrder.METRICS

    override fun Application.install() {
        install(MicrometerMetrics) { registry = meterRegistry }
    }
}
