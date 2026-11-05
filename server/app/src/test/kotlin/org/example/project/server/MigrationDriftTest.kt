package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.dbTransaction
import org.example.project.server.testing.buildTestGraph
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * The migration drift safety net: apply every Flyway migration to a throwaway Postgres, then assert
 * the live schema already matches the aggregated Exposed `Table`s — i.e. no migration statements
 * are *still required*. A non-empty result means the code-side schema and the SQL migrations have
 * drifted (someone added/changed a `Table` but forgot the migration), and the build goes red
 * instead of the surprise surfacing in production.
 *
 * It reads `graph.tableSets`, so it extends to every domain automatically through the multibinding.
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
            val graph = buildTestGraph(databaseConfig)
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
