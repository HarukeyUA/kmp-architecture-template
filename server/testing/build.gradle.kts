import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * The in-process server harness: a process-wide Testcontainers Postgres/MinIO, the booted
 * `:server:*` Ktor stack ([installTestServer]), the seam [io.ktor.client.HttpClient] the
 * `:server:app` suites speak, and the auth fixtures. Plain kotlin-jvm (not KMP) because every
 * consumer is a JVM test source set wiring real Ktor + Testcontainers + the server graph.
 *
 * Kept out of the module-graph asserts (like `:server:app` is unrestricted) so it can declare the
 * cross-cutting server dependencies the ":testing depends only on its sibling :public" rule would
 * otherwise reject — this is the one harness module, not a per-contract fake. `convention.metro` is
 * applied because [buildTestGraph] calls Metro's `createGraphFactory` intrinsic.
 */
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.convention.metro)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    // The `:server:*` stack, booted in-process via configureServer(buildTestGraph(...)). This
    // dependency is also the aggregator: convention.impl-aggregator exports every :server:*:impl
    // from the app as `api`, and each domain chain (impl → public → :shared:<domain> →
    // :shared:common) is `api` all the way down — so the config types, the seam contracts, and
    // every new domain's DTOs/lenses reach the harness and its consumers with no hand-maintained
    // module registry here. Only lifecycle:public sits outside that chain (the app holds it as
    // plain `implementation`), and it is real harness ABI: serverTest's `finally` calls closeAll.
    api(project(":server:app"))
    api(project(":server:core:lifecycle:public"))

    api(libs.ktor.server.test.host)
    api(libs.ktor.client.core)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.client.resources)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.minio)
    api(libs.aws.sdk.kotlin.s3)
    api(libs.exposed.jdbc)
    api(libs.hikari)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)
}
