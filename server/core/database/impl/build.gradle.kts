plugins { alias(libs.plugins.convention.server.core.impl) }

dependencies {
    // Contributes a DB health indicator into the observability contract.
    implementation(project(":server:core:observability:public"))
    implementation(project(":server:core:lifecycle:public"))

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
}
