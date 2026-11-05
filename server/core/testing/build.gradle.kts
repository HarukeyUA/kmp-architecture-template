import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Domain-agnostic building blocks for the per-slice server suites. Plain kotlin-jvm because every
 * consumer is a JVM test source set. Kept out of the module-graph asserts so `test` source sets
 * across `:server:*` can depend on it despite the ":testing depends only on its sibling" rule.
 */
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
    compilerOptions { jvmTarget.set(JvmTarget.fromTarget(libs.versions.jdk.get())) }
}

dependencies {
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.testcontainers.core)
    api(libs.testcontainers.postgresql)
    runtimeOnly(libs.postgresql)
}
