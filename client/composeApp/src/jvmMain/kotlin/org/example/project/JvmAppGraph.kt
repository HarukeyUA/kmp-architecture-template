package org.example.project

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfigDefaults

@DependencyGraph(AppScope::class)
interface JvmAppGraph : AppGraph {
    /** Desktop always runs on the dev machine itself, so its dev default is plain localhost. */
    @Provides
    fun provideApiConfigDefaults(): ApiConfigDefaults =
        ApiConfigDefaults(devBaseUrl = "http://localhost:8080", prodBaseUrl = PROD_SERVER_BASE_URL)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides environment: Environment): JvmAppGraph
    }
}
