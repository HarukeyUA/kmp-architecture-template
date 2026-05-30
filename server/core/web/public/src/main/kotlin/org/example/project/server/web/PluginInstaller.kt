package org.example.project.server.web

import io.ktor.server.application.Application

/**
 * A self-registering Ktor application plugin (ContentNegotiation, CallId, metrics, …). Like
 * [RouteRegistrar], implementations are contributed via `@ContributesIntoSet(AppScope::class)` and
 * installed by `:server:app`. Installed in ascending [order].
 */
interface PluginInstaller {
    val order: PluginOrder

    fun Application.install()
}

/**
 * Deterministic install order for [PluginInstaller]s (sorted by declaration order). Correlation id
 * must precede logging so the request id is in the MDC; content negotiation must precede routes;
 * the status-pages safety net wraps everything; auth is installed last before routes run.
 */
enum class PluginOrder {
    CALL_ID,
    MONITORING,
    METRICS,
    CONTENT_NEGOTIATION,
    RESOURCES,
    STATUS_PAGES,
    AUTHENTICATION,
}
