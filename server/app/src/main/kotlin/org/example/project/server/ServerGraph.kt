package org.example.project.server

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import org.example.project.server.auth.JwtConfig
import org.example.project.server.auth.SessionStore
import org.example.project.server.database.DatabaseBootstrap
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.TableSet
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.ServerResource
import org.example.project.server.observability.MetricsConfig
import org.example.project.server.scheduler.ScheduledJob
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.WebLimitsConfig

/**
 * The server composition root (analogous to the client's `JvmAppGraph`). Metro merges every
 * `@ContributesIntoSet` / `@ContributesTo` contribution from the aggregated `:server:*:impl`
 * modules, so the multibound sets below assemble themselves — adding a domain never edits this
 * file.
 *
 * The sets are declared `allowEmpty = true` so a domain-less server still boots; [tableSets] is
 * empty until the first domain ships a table.
 */
@DependencyGraph(AppScope::class)
interface ServerGraph {
    val config: ServerConfig
    val databaseBootstrap: DatabaseBootstrap

    /**
     * The invariant session primitive, exposed so revocation flows without a route yet (e.g.
     * [SessionStore.revokeAllFor]) are exercisable by the integration suite.
     */
    val sessionStore: SessionStore

    /**
     * The contributed background jobs. Exposed so a test can assert the set every domain merged
     * into and drive a sweep without waiting out its interval.
     */
    val scheduledJobs: Set<ScheduledJob>

    @Multibinds(allowEmpty = true) val pluginInstallers: Set<PluginInstaller>

    @Multibinds(allowEmpty = true) val routeRegistrars: Set<RouteRegistrar>

    @Multibinds(allowEmpty = true) val tableSets: Set<TableSet>

    @Multibinds(allowEmpty = true) val serverResources: Set<ServerResource>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides config: ServerConfig,
            @Provides databaseConfig: DatabaseConfig,
            @Provides metricsConfig: MetricsConfig,
            @Provides webLimitsConfig: WebLimitsConfig,
            @Provides jwtConfig: JwtConfig,
            @Provides environment: ServerEnvironment,
        ): ServerGraph
    }
}
