package org.example.project.server.database

import dev.zacsweers.metro.Inject
import javax.sql.DataSource
import org.flywaydb.core.Flyway

/**
 * Applies every SQL migration found under `db/migration` on the classpath. Each domain co-locates
 * its timestamp-versioned migrations in its own `:impl` resources (`V20260530__add_x.sql`); because
 * they all land at the same classpath location, Flyway sees the merged, chronologically-ordered set
 * with no central migrations folder (ADR-0007).
 */
@Inject
class FlywayMigrator(private val dataSource: DataSource) {
    fun migrate() {
        Flyway.configure()
            .dataSource(dataSource)
            .locations(MIGRATIONS_LOCATION)
            .validateMigrationNaming(true)
            .outOfOrder(false)
            .load()
            .migrate()
    }

    private companion object {
        const val MIGRATIONS_LOCATION = "classpath:db/migration"
    }
}
