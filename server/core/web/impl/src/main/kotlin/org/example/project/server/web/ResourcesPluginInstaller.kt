package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources

/**
 * Enables typed `@Resource` routing on the server, so a domain's routes can `get<MyResource>` /
 * `post<MyResource>` against the same `@Resource` classes the client uses (ADR-0002). Must be
 * installed before routes register — every [PluginInstaller] is, so any pre-route [PluginOrder]
 * works.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class ResourcesPluginInstaller : PluginInstaller {
    override val order: PluginOrder = PluginOrder.RESOURCES

    override fun Application.install() {
        install(Resources)
    }
}
