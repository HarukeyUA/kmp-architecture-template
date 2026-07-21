package org.example.project.server.observability

/**
 * A health probe contributed by an infrastructure or domain module via
 * `@ContributesIntoSet(AppScope::class)`. The `/health` endpoint aggregates the whole
 * `Set<HealthIndicator>` (DB connectivity, etc.). Keep [check] cheap — it runs on every probe.
 */
fun interface HealthIndicator {
    suspend fun check(): HealthStatus
}

/** Outcome of a single [HealthIndicator]. */
data class HealthStatus(val name: String, val healthy: Boolean, val detail: String? = null)
