plugins { alias(libs.plugins.convention.server.core.impl) }

dependencies {
    // The PluginInstaller contract: the scheduler self-registers and starts on app launch.
    implementation(project(":server:core:web:public"))

    // The background loops live on the Ktor application's coroutine scope (cancelled on shutdown).
    implementation(libs.ktor.server.core)

    // The advisory-lock test spins up two scheduler instances against one Postgres.
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.hikari)
    testImplementation(libs.postgresql)
}
