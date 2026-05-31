plugins { alias(libs.plugins.convention.server.core.impl) }

dependencies {
    // sessions table + tx helper + TableSet, and the PluginInstaller contract for the auth
    // middleware.
    implementation(project(":server:core:database:public"))
    implementation(project(":server:core:web:public"))
    // The expired-session sweep is contributed as a ScheduledJob; the advisory-lock scheduler runs
    // it.
    implementation(project(":server:core:scheduler:public"))

    implementation(libs.ktor.server.auth)
    implementation(libs.caffeine)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
}
