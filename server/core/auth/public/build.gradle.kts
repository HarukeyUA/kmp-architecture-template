plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.auth)
}
