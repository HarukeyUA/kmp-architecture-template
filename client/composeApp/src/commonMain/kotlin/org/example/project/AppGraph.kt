package org.example.project

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfig
import org.example.project.core.network.ApiConfigDefaults

interface AppGraph {
    /**
     * The base URL every request rides, resolved once from injected build-time facts: [Environment]
     * from the platform entry point, [ApiConfigDefaults] from the platform graph.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideApiConfig(environment: Environment, defaults: ApiConfigDefaults): ApiConfig =
        resolveApiConfig(environment, defaults)

    val rootComponentFactory: RootComponent.Factory
    val rootScreen: RootScreen
}
