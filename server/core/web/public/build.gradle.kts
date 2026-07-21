plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    api(project(":shared:common"))

    api(libs.ktor.server.core)
    implementation(libs.ktor.server.call.id)
    api(libs.ktor.server.resources)
}
