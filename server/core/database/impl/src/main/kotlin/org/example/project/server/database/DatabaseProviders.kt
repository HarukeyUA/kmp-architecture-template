package org.example.project.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import javax.sql.DataSource
import org.example.project.server.lifecycle.ServerResource

// Upper bound on how long pool construction keeps retrying the first connection. A managed Postgres
// that sleeps when idle takes seconds to wake, so a single attempt would fail the boot.
private const val POOL_INITIALIZATION_TIMEOUT_MILLIS: Long = 60_000

private const val CONNECTION_TIMEOUT_MILLIS: Long = 5_000

@ContributesTo(AppScope::class)
interface DatabaseProviders {
    /**
     * The single HikariCP pool, sized by config. `instances × maxPoolSize` must stay below Postgres
     * `max_connections` when scaling out; the pool is the only thing that holds connections, so the
     * app itself stays stateless.
     */
    // The pool outlives this call: `dataSourceResource` below closes it at shutdown.
    @Suppress("MissingUseCall")
    @Provides
    @SingleIn(AppScope::class)
    fun provideDataSource(config: DatabaseConfig): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                config.username?.let { username = it }
                config.password?.let { password = it }
                maximumPoolSize = config.maxPoolSize
                driverClassName = "org.postgresql.Driver"
                connectionTimeout = CONNECTION_TIMEOUT_MILLIS
                initializationFailTimeout = POOL_INITIALIZATION_TIMEOUT_MILLIS
            },
        )

    /** Releases the app's single connection pool when the server resource scope exits. */
    @Provides
    @IntoSet
    fun dataSourceResource(dataSource: DataSource): ServerResource = ServerResource {
        (dataSource as? HikariDataSource)?.close() ?: (dataSource as? AutoCloseable)?.close()
    }
}
