plugins { alias(libs.plugins.convention.server.core.impl) }

dependencies {
    // The HTTP contracts (PluginInstaller / RouteRegistrar) this module contributes into.
    implementation(project(":server:core:web:public"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    // /health renders a small JSON body via the kotlinx-serialization runtime (buildJsonObject).
    implementation(libs.kotlinx.serialization.json)
}
