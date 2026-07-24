package org.example.project

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfigDefaults

@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides applicationContext: Context,
            @Provides environment: Environment,
            @Provides apiConfigDefaults: ApiConfigDefaults,
        ): AndroidAppGraph
    }
}

/**
 * [devServerHost] is the build machine's LAN IP, resolved while assembling the dev flavor and
 * carried in its `BuildConfig` — a freshly installed dev build on a physical device (or the
 * emulator, which can also reach the host's LAN IP) finds the local server with zero setup. Null or
 * unusable falls back to `10.0.2.2`, the emulator's NAT alias for the host's loopback.
 */
fun createAndroidAppGraph(
    applicationContext: Context,
    environment: Environment,
    devServerHost: String?,
): AndroidAppGraph =
    createGraphFactory<AndroidAppGraph.Factory>()
        .create(
            applicationContext,
            environment,
            devApiConfigDefaults(devServerHost, fallbackDevBaseUrl = "http://10.0.2.2:8080"),
        )
