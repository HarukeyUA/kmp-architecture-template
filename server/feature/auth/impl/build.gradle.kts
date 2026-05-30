plugins { alias(libs.plugins.convention.server.feature.impl) }

dependencies {
    // Credential module (Argon2id) issues a Session via core:auth; routes use respondEither + the
    // typed @Resource; the accounts table uses the tx helper + Instant timestamps.
    implementation(project(":server:core:auth:public"))
    implementation(project(":server:core:database:public"))
    implementation(project(":server:core:web:public"))

    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auth)
    implementation(libs.argon2)
    implementation(libs.exposed.kotlin.datetime)
}
