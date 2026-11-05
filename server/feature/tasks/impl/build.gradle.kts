plugins { alias(libs.plugins.convention.server.feature.impl) }

dependencies {
    implementation(project(":server:core:auth:public"))
    implementation(project(":server:core:database:public"))
    implementation(project(":server:core:web:public"))

    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auth)
    implementation(libs.exposed.kotlin.datetime)
}
