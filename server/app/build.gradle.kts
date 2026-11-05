import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

plugins { alias(libs.plugins.convention.server.app) }

// All `:server:*:impl` modules are wired in automatically by convention.impl-aggregator, so Metro
// can merge their @ContributesIntoSet route/table/error contributions into the server graph.
// Adding a domain touches zero lines here.

dependencies {
    implementation(project(":server:core:lifecycle:public"))

    // The integration suites boot the stack through :server:testing's single-boot `serverTest`
    // harness (shared Testcontainers Postgres, seam client, auth fixtures).
    testImplementation(project(":server:testing"))

    // Migration drift test: diff the aggregated Exposed schema (graph.tableSets) against a
    // Testcontainers Postgres with all migrations applied. Lives here because only the app graph
    // assembles every domain's tables.
    testImplementation(libs.exposed.migration.jdbc)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
}

val testRuntimeClasspath =
    extensions.getByType<SourceSetContainer>().getByName("test").runtimeClasspath

tasks.register<JavaExec>("printMigrationDraft") {
    group = "database"
    description =
        "Prints SQL required to align Flyway migrations with the aggregated Exposed schema."
    classpath = testRuntimeClasspath
    mainClass.set("org.example.project.server.MigrationDraftCliKt")
    dependsOn(tasks.named("testClasses"))
    systemProperty(
        "logback.configurationFile",
        layout.projectDirectory.file("src/test/resources/migration-draft-logback.xml").asFile,
    )

    args("--mode", "print")
}

tasks.register<JavaExec>("generateMigrationDraft") {
    group = "database"
    description =
        "Writes a reviewed-before-commit Flyway migration draft for the aggregated Exposed schema."
    classpath = testRuntimeClasspath
    mainClass.set("org.example.project.server.MigrationDraftCliKt")
    dependsOn(tasks.named("testClasses"))
    systemProperty(
        "logback.configurationFile",
        layout.projectDirectory.file("src/test/resources/migration-draft-logback.xml").asFile,
    )

    val migrationDescription = providers.gradleProperty("migrationDescription").orNull
    val migrationOutput =
        providers.gradleProperty("migrationOutput").orNull?.let { file(it).absolutePath }
    val migrationOutputDir =
        providers.gradleProperty("migrationModule").orNull?.let { module ->
            // Accepts a project path or a relative directory; resolved from the root directory
            // because isolated projects forbid reaching into another project's model.
            isolated.rootProject.projectDirectory
                .dir(module.removePrefix(":").replace(':', '/'))
                .dir("src/main/resources/db/migration")
                .asFile
                .absolutePath
        }

    doFirst {
        val description =
            migrationDescription ?: error("Pass -PmigrationDescription=create_widgets")

        args("--mode", "write", "--description", description)

        when {
            migrationOutput != null -> args("--output", migrationOutput)
            migrationOutputDir != null -> args("--output-dir", migrationOutputDir)
            else ->
                error(
                    "Pass -PmigrationModule=:server:feature:widgets:impl " +
                        "or -PmigrationOutput=/path/to/V...__description.sql",
                )
        }
    }
}
