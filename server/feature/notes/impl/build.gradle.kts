plugins { alias(libs.plugins.convention.server.feature.impl) }

dependencies {
    // Cross-domain (ADR-0006): the notes service calls AuthService through its *public* contract to
    // resolve the author's email — never the auth :impl or its accounts table. Swapping this for
    // ":server:feature:auth:impl" is rejected by assertModuleDependencies (the Phase-5 boundary
    // proof).
    implementation(project(":server:feature:auth:public"))

    implementation(project(":server:core:auth:public"))
    implementation(project(":server:core:database:public"))
    implementation(project(":server:core:web:public"))

    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auth)
    implementation(libs.exposed.kotlin.datetime)
}
