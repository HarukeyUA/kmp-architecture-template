package org.example.project.server

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import org.example.project.server.database.DatabaseBootstrap
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.TableSet
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.RouteRegistrar

/**
 * The server composition root (analogous to the client's `JvmAppGraph`). Metro merges every
 * `@ContributesIntoSet` / `@ContributesTo` contribution from the aggregated `:server:*:impl`
 * modules, so the multibound sets below assemble themselves — adding a domain never edits this file
 * (ADR-0006, ADR-0008).
 *
 * The sets are declared `allowEmpty = true` so a domain-less server still boots; [tableSets] is
 * empty until the first domain ships a table.
 */
@DependencyGraph(AppScope::class)
interface ServerGraph {
    val config: ServerConfig
    val databaseBootstrap: DatabaseBootstrap

    @Multibinds(allowEmpty = true) val pluginInstallers: Set<PluginInstaller>

    @Multibinds(allowEmpty = true) val routeRegistrars: Set<RouteRegistrar>

    @Multibinds(allowEmpty = true) val tableSets: Set<TableSet>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides config: ServerConfig,
            @Provides databaseConfig: DatabaseConfig,
        ): ServerGraph
    }
}
