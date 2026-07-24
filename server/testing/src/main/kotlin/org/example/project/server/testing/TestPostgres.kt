package org.example.project.server.testing

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.FlywayMigrator
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Process-wide Testcontainers Postgres for integration tests. The container starts on first access
 * and is reused across tests in the JVM; [resetSchema] truncates the data between runs so we don't
 * pay the Flyway tax more than once.
 *
 * Bootstrapping mirrors production: the shared pool is Flyway-migrated by [FlywayMigrator] (the
 * same merged `classpath:db/migration` set the domains ship), then Exposed is connected to it. The
 * server graph the harness boots runs its services against this ambient Exposed default rather than
 * opening a pool of its own — so [connect] must run before each `testApplication` block (see
 * [installTestServer]).
 */
object TestPostgres {
    private val container: PostgreSQLContainer<*> by lazy {
        val c = PostgreSQLContainer("postgres:17-alpine").withReuse(false)
        c.start()
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { c.stop() } })
        c
    }
    private var dataSourceRef: HikariDataSource? = null

    fun dataSource(): DataSource {
        dataSourceRef?.let {
            return it
        }
        val c = container
        val ds =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = c.jdbcUrl
                    username = c.username
                    password = c.password
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 5
                    connectionTimeout = 5_000
                }
            )
        FlywayMigrator(ds).migrate()
        dataSourceRef = ds
        return ds
    }

    /**
     * (Re)binds the shared pool as Exposed's ambient default and returns it. Exposed resolves an
     * un-parameterized `suspendTransaction` to the *last* `Database` created, so this must run anew
     * before every harness boot: a sibling suite that connects its own throwaway database first —
     * `MigrationDriftTest` does, via `databaseBootstrap.start()` against a `.use`-scoped container
     * that is gone by the time it finishes — would otherwise leave that dead database as the
     * ambient default and every following request would fail against it.
     */
    fun connect(): Database = Database.connect(dataSource())

    /**
     * The graph factory's [DatabaseConfig] for the shared container (jdbc-form URL, split creds).
     */
    fun databaseConfig(): DatabaseConfig {
        val c = container
        return DatabaseConfig(jdbcUrl = c.jdbcUrl, username = c.username, password = c.password)
    }

    /**
     * Truncates every base table in the `public` schema (except Flyway's own history) so a new
     * migration adding a table can never silently escape the reset. Runs after the pool is
     * migrated, so the list is derived from the live catalog rather than a hardcoded enumeration.
     */
    fun resetSchema() {
        val ds = dataSource()
        ds.connection.use { conn ->
            val tables = mutableListOf<String>()
            conn
                .prepareStatement(
                    "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "AND table_name <> 'flyway_schema_history'"
                )
                .use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) tables.add(rs.getString("table_name"))
                    }
                }
            if (tables.isEmpty()) return
            val truncateList = tables.joinToString(", ") { "\"$it\"" }
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE $truncateList RESTART IDENTITY CASCADE")
            }
        }
    }
}
