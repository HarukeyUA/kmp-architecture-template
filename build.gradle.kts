// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.detekt)
}

tasks.register<Exec>("checkMigrationOrder") {
    group = "verification"
    description =
        "Fails when PR-added Flyway migrations are older than the latest migration on the base ref."

    val baseRef =
        providers
            .gradleProperty("migrationBaseRef")
            .orElse(providers.environmentVariable("MIGRATION_BASE_REF"))
            .orElse("origin/main")

    doFirst { commandLine("bash", "scripts/check-migration-order.sh", "--base", baseRef.get()) }
}
