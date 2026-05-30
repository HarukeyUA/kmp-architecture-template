plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    // Principal, the authenticate{} wrapper, and the session-store interface use Ktor auth types.
    api(libs.ktor.server.core)
    api(libs.ktor.server.auth)
}
