package org.example.project

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import org.example.project.core.buildinfo.Environment
import org.example.project.core.network.ApiConfigDefaults

@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides environment: Environment,
            @Provides apiConfigDefaults: ApiConfigDefaults,
        ): IosAppGraph
    }
}

/**
 * `isDev` rather than [Environment] because this is the ObjC seam: a Bool crosses it verbatim,
 * while the enum would surface in Swift under an export-mangled name. The value comes from the
 * `DEV` compilation condition on the `-dev` build configurations (`#if DEV` at the call site) — the
 * environment is a compile-time fact of the scheme, never sniffed from the bundle id.
 *
 * [devServerHost] is the build Mac's Bonjour name (or LAN IP), read from the dev plist's
 * `DevServerHost` key — see Info-dev.plist. Null/blank falls back to `localhost`, which is the Mac
 * itself on the simulator; a physical iPhone relies on the Bonjour/LAN host being reachable.
 */
fun createAppGraph(isDev: Boolean, devServerHost: String?): IosAppGraph =
    createGraphFactory<IosAppGraph.Factory>()
        .create(
            if (isDev) Environment.DEV else Environment.PROD,
            devApiConfigDefaults(devServerHost, fallbackDevBaseUrl = "http://localhost:8080"),
        )
