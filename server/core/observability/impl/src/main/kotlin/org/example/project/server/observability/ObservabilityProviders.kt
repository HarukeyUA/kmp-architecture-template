package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

@ContributesTo(AppScope::class)
interface ObservabilityProviders {
    /** Shared Prometheus registry — fed by the Ktor metrics plugin and scraped by `/metrics`. */
    @Provides
    @SingleIn(AppScope::class)
    fun providePrometheusMeterRegistry(): PrometheusMeterRegistry =
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
