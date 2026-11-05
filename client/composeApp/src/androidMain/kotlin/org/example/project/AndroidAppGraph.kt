package org.example.project

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfigDefaults

/**
 * What the Android app pulls out of DI. Not a graph itself: Metro forbids one `@DependencyGraph`
 * extending another, so the shipped graph and `E2eAppGraph` (in `:client:androidApp`'s
 * `androidTest`) are siblings under this contract.
 */
interface AndroidAppGraph : AppGraph

/** The graph a shipped build runs on; reachable only through [createAndroidAppGraph]. */
@DependencyGraph(AppScope::class)
internal interface ProdAndroidAppGraph : AndroidAppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides applicationContext: Context,
            @Provides environment: Environment,
            @Provides apiConfigDefaults: ApiConfigDefaults,
        ): ProdAndroidAppGraph
    }
}

fun createAndroidAppGraph(
    applicationContext: Context,
    environment: Environment,
    devServerHost: String?,
): AndroidAppGraph =
    createGraphFactory<ProdAndroidAppGraph.Factory>()
        .create(applicationContext, environment, androidApiConfigDefaults(devServerHost))

/**
 * The base URLs an Android build talks to. [devServerHost] is the build machine's LAN IP, baked
 * into the dev flavor's `BuildConfig`; null or unusable falls back to 10.0.2.2, the emulator's NAT
 * alias for the host loopback. Public so the E2E runner's own graph reaches the same server.
 */
fun androidApiConfigDefaults(devServerHost: String?): ApiConfigDefaults =
    devApiConfigDefaults(devServerHost, fallbackDevBaseUrl = "http://10.0.2.2:8080")
