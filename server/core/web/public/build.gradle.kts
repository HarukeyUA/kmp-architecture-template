plugins { alias(libs.plugins.convention.server.core.public) }

dependencies {
    // RouteRegistrar / PluginInstaller carry Ktor receiver types (Application).
    api(libs.ktor.server.core)
}
