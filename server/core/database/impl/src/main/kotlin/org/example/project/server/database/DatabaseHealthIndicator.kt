package org.example.project.server.database

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import org.example.project.server.observability.HealthIndicator
import org.example.project.server.observability.HealthStatus

/** Reports DB connectivity to `/health` by running a trivial query through the connection pool. */
@Inject
@ContributesIntoSet(AppScope::class)
class DatabaseHealthIndicator : HealthIndicator {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun check(): HealthStatus =
        try {
            dbTransaction { exec("SELECT 1") }
            HealthStatus(name = "database", healthy = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HealthStatus(name = "database", healthy = false, detail = e.message)
        }
}
