package org.example.project.server.observability

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.origin
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
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
        val detailed = log.isDebugEnabled
        install(CallLogging) {
            mdc(Correlation.MDC_KEY) { call -> call.callId }
            format { call -> call.toLogMessage(detailed) }
        }
    }
}

// Paths deliberately exclude query strings, and headers and bodies are never logged; the DEBUG
// diagnostics stay in that bound.
private fun ApplicationCall.toLogMessage(detailed: Boolean): String = buildString {
    append(response.status() ?: "Unhandled")
    append(": ")
    append(request.httpMethod.value)
    append(" - ")
    append(request.path())
    append(" in ")
    append(processingTimeMillis())
    append("ms")
    if (detailed) {
        append(" [remote=")
        append(request.origin.remoteHost)
        append(", contentType=")
        append(request.contentType())
        append(", contentLength=")
        append(request.contentLength() ?: "unknown")
        append(']')
    }
}
