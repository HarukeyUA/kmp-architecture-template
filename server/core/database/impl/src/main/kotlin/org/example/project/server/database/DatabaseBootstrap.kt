package org.example.project.server.database

import dev.zacsweers.metro.Inject
import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Brings persistence online at startup: applies Flyway migrations (the DB is the SQL source of
 * truth), then connects Exposed to the same pool (Exposed `Table`s are the code source of truth).
 * Invoked once from `main()` before the server begins serving.
 */
@Inject
class DatabaseBootstrap(private val dataSource: DataSource, private val migrator: FlywayMigrator) {
    fun start() {
        migrator.migrate()
        Database.connect(dataSource)
    }
}
