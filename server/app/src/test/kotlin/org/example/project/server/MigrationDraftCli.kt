package org.example.project.server

import dev.zacsweers.metro.createGraphFactory
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.dbTransaction
import org.example.project.server.observability.MetricsConfig
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.testcontainers.containers.PostgreSQLContainer

fun main(args: Array<String>) {
    val options = MigrationDraftOptions.parse(args.toList())
    val statements = generateMigrationStatements()

    if (statements.isEmpty()) {
        println("No migration required; Exposed schema matches applied Flyway migrations.")
        return
    }

    val draft = statements.toMigrationDraft()

    when (options.mode) {
        MigrationDraftMode.Print -> print(draft)
        MigrationDraftMode.Write -> {
            val output = options.outputPath()
            check(!output.exists()) { "Refusing to overwrite existing migration: $output" }

            output.parent.createDirectories()
            output.writeText(draft)
            println("Wrote migration draft to $output")
        }
    }
}

private fun generateMigrationStatements(): List<String> {
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
                version = "migration-draft",
                database = databaseConfig,
                storage = storageConfig,
                metrics = MetricsConfig(port = 0),
                webLimits = testWebLimitsConfig(),
            )

        val graph =
            createGraphFactory<ServerGraph.Factory>()
                .create(config, databaseConfig, storageConfig, config.metrics, config.webLimits)

        graph.databaseBootstrap.start()

        val tables = graph.tableSets.flatMap { it.tables }.toTypedArray()
        return runBlocking {
            dbTransaction {
                MigrationUtils.statementsRequiredForDatabaseMigration(*tables, withLogs = false)
            }
        }
    }
}

private fun List<String>.toMigrationDraft(): String = buildString {
    appendLine("-- Generated migration draft. Review before committing.")
    appendLine("-- Source: Exposed MigrationUtils against fresh Postgres after applying Flyway.")
    appendLine()

    this@toMigrationDraft.forEach { statement ->
        appendLine(statement.trim().removeSuffix(";") + ";")
        appendLine()
    }
}

private data class MigrationDraftOptions(
    val mode: MigrationDraftMode,
    val description: String?,
    val output: Path?,
    val outputDir: Path?,
) {
    companion object {
        fun parse(args: List<String>): MigrationDraftOptions {
            var mode: MigrationDraftMode? = null
            var description: String? = null
            var output: Path? = null
            var outputDir: Path? = null

            val iterator = args.iterator()
            while (iterator.hasNext()) {
                when (val option = iterator.next()) {
                    "--mode" -> mode = MigrationDraftMode.parse(iterator.nextRequired(option))
                    "--description" -> description = iterator.nextRequired(option)
                    "--output" -> output = Path.of(iterator.nextRequired(option))
                    "--output-dir" -> outputDir = Path.of(iterator.nextRequired(option))
                    else -> error("Unknown option: $option")
                }
            }

            return MigrationDraftOptions(
                mode = checkNotNull(mode) { "Missing --mode" },
                description = description,
                output = output,
                outputDir = outputDir,
            )
        }
    }
}

private fun MigrationDraftOptions.outputPath(): Path {
    output?.let {
        return it
    }
    val dir = checkNotNull(outputDir) { "Missing --output or --output-dir" }
    val safeDescription = description.toMigrationDescription()
    val version = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    return dir.resolve("V${version}__${safeDescription}.sql")
}

private enum class MigrationDraftMode {
    Print,
    Write;

    companion object {
        fun parse(value: String): MigrationDraftMode =
            when (value) {
                "print" -> Print
                "write" -> Write
                else -> error("Unknown --mode value: $value")
            }
    }
}

private fun Iterator<String>.nextRequired(option: String): String =
    if (hasNext()) next() else error("Missing value for $option")

private fun String?.toMigrationDescription(): String {
    val description =
        checkNotNull(this) { "Missing --description" }
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    require(description.isNotBlank()) { "Migration description must contain a letter or digit" }
    return description
}
