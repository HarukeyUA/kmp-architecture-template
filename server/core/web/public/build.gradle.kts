plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    // RouteRegistrar / PluginInstaller carry Ktor receiver types (Application).
    api(libs.ktor.server.core)
    // ApiError / ErrorEnvelope appear on the responder's public surface.
    api(project(":shared:common"))
    // callId supplies the requestId echoed in the ErrorEnvelope.
    implementation(libs.ktor.server.call.id)
}
