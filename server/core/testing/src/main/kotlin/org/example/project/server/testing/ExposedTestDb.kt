package org.example.project.server.testing

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * A Testcontainers Postgres shared across a slice's repository tests, started once per JVM; [reset]
 * truncates exactly [tables]. Tables are built via [SchemaUtils.create], not Flyway, because a
 * slice knows only its own; `MigrationDriftTest` guards them against the migrations.
 */
class ExposedTestDb(private vararg val tables: Table) {
    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer("postgres:17-alpine").also {
            it.start()
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { it.stop() } })
        }
    }

    // Exposed takes the tables as a vararg, so forwarding them costs one array copy per JVM.
    @Suppress("SpreadOperator")
    private val database: Database by lazy {
        val db =
            Database.connect(
                url = container.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = container.username,
                password = container.password,
            )
        transaction(db) { SchemaUtils.create(*tables) }
        db
    }

    fun connect(): Database = database

    fun reset() {
        val truncateList = tables.joinToString(", ") { it.tableName }
        transaction(database) { exec("TRUNCATE TABLE $truncateList RESTART IDENTITY CASCADE") }
    }
}
