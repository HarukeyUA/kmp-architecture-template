package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/**
 * Structured request logging with the correlation id in the MDC (installed after
 * [CallIdPluginInstaller]).
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CallLoggingPluginInstaller : PluginInstaller {
    override val order: PluginOrder = PluginOrder.MONITORING

    override fun Application.install() {
        install(CallLogging) { mdc(Correlation.MDC_KEY) { call -> call.callId } }
    }
}
