package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEmpty
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.dbTransaction
import org.example.project.server.observability.MetricsConfig
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The migration drift safety net (ADR-0007): apply every Flyway migration to a throwaway Postgres,
 * then assert the live schema already matches the aggregated Exposed `Table`s — i.e. no migration
 * statements are *still required*. A non-empty result means the code-side schema and the SQL
 * migrations have drifted (someone added/changed a `Table` but forgot the migration), and the build
 * goes red instead of the surprise surfacing in production.
 *
 * It reads `graph.tableSets`, so it extends to every domain automatically through the multibinding.
 * Phase 2 has no tables yet, so it proves the harness works on an empty schema.
 */
class MigrationDriftTest {
    @Test
    fun `exposed schema has no drift from applied migrations`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()

            val databaseConfig =
                DatabaseConfig(
                    jdbcUrl = postgres.jdbcUrl,
                    username = postgres.username,
                    password = postgres.password,
                )
            val storageConfig = testStorageConfig()
            val config =
                ServerConfig(
                    host = "localhost",
                    port = 0,
                    version = "test",
                    database = databaseConfig,
                    storage = storageConfig,
                    metrics = MetricsConfig(port = 0),
                )

            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(config, databaseConfig, storageConfig, config.metrics)
            // Applies Flyway migrations and connects Exposed against the container.
            graph.databaseBootstrap.start()

            val tables = graph.tableSets.flatMap { it.tables }.toTypedArray()
            val requiredStatements = runBlocking {
                dbTransaction {
                    MigrationUtils.statementsRequiredForDatabaseMigration(*tables, withLogs = false)
                }
            }

            assertThat(requiredStatements).isEmpty()
        }
    }
}
