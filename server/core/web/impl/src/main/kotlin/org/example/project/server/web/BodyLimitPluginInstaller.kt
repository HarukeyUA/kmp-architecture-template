package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit

/**
 * Caps every request body at [WebLimitsConfig.maxRequestBodyBytes] (ADR-0010 §11). A declared
 * `Content-Length` over the limit is rejected before the body is read; a streaming body is cut off
 * at the limit mid-receive. Both surface as `PayloadTooLargeException`, which StatusPages maps to
 * the typed `PayloadTooLarge` envelope (413) — without that handler the exception would fall into
 * the `ContentTransformationException` path and lie to the client with a 400 "malformed_body".
 */
@Inject
@ContributesIntoSet(AppScope::class)
class BodyLimitPluginInstaller(private val limits: WebLimitsConfig) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.BODY_LIMIT

    override fun Application.install() {
        install(RequestBodyLimit) { bodyLimit { limits.maxRequestBodyBytes } }
    }
}
