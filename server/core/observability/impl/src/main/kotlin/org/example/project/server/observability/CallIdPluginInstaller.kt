package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import java.util.UUID
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/**
 * Assigns/propagates the per-request correlation id (read from / echoed to [Correlation.HEADER]).
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CallIdPluginInstaller : PluginInstaller {
    override val order: PluginOrder = PluginOrder.CALL_ID

    override fun Application.install() {
        install(CallId) {
            header(Correlation.HEADER)
            generate { UUID.randomUUID().toString() }
            verify { it.isNotBlank() }
        }
    }
}
