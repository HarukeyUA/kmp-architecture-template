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

@ContributesTo(AppScope::class)
interface DatabaseProviders {
    /**
     * The single HikariCP pool, sized by config. `instances × maxPoolSize` must stay below Postgres
     * `max_connections` when scaling out (ADR-0010); the pool is the only thing that holds
     * connections, so the app itself stays stateless.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideDataSource(config: DatabaseConfig): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.username
                password = config.password
                maximumPoolSize = config.maxPoolSize
                driverClassName = "org.postgresql.Driver"
            }
        )

    /** Releases the app's single connection pool when the server resource scope exits. */
    @Provides
    @IntoSet
    fun dataSourceResource(dataSource: DataSource): ServerResource = ServerResource {
        (dataSource as? HikariDataSource)?.close() ?: (dataSource as? AutoCloseable)?.close()
    }
}
