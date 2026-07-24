import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Client↔server integration tests — the one module where both worlds may meet. It lives in the
 * test-only `:integration` umbrella (may depend on everything; nothing may depend on it), so the
 * `:client` tree never carries a `:server` dependency, not even a test one.
 *
 * The suites drive real client repositories (`:client:feature:*:impl`, JVM variant) over
 * `:server:testing`'s in-process test transport: the `testApplication` client is the wire straight
 * into the booted `:server:*` stack, so a repository round trip proves the seam contract — typed
 * `@Resource` routes, seam `Json`, sealed-lens Declared errors — with no engine, no network, no
 * running dev stack.
 */
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.convention.spotless)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.convention.module.graph.assert)
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    // The in-process server harness (serverTest, TestPostgres, seam client) — and, through its
    // :server:app api chain, the seam contracts.
    testImplementation(project(":server:testing"))

    // The real client data layer under test: repositories + the bearer/session machinery they
    // ride. Depending on :impl is legal only here — :integration sees everything.
    testImplementation(project(":client:feature:auth:impl"))
    testImplementation(project(":client:feature:notes:impl"))
    testImplementation(project(":client:core:network:public"))
    testImplementation(project(":client:core:secure-storage:public"))

    // The server-side quota constant, asserted against through the client repository.
    testImplementation(project(":server:feature:notes:public"))

    testImplementation(libs.ktor.client.auth)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.test)
}
