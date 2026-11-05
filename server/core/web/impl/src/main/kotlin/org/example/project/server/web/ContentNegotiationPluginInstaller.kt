package org.example.project.server.web

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

@Inject
@ContributesIntoSet(AppScope::class)
class ContentNegotiationPluginInstaller(private val json: Json) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.CONTENT_NEGOTIATION

    override fun Application.install() {
        install(ContentNegotiation) { json(json) }
    }
}
