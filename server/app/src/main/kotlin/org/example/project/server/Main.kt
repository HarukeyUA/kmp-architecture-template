package org.example.project.server

import dev.zacsweers.metro.createGraphFactory
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey

/**
 * The live [ServerGraph], stashed on the application so integration tests can reach real services.
 */
val ServerGraphKey: AttributeKey<ServerGraph> = AttributeKey("ServerGraph")

fun main() {
    val config = ServerConfig.load()
    val graph =
        createGraphFactory<ServerGraph.Factory>().create(config, config.database, config.storage)
    graph.databaseBootstrap.start()
    embeddedServer(Netty, port = config.port, host = config.host) { configureServer(graph) }
        .start(wait = true)
}

/**
 * Installs the self-registered plugins (in [org.example.project.server.web.PluginOrder]) and routes
 * from [graph]. Kept separate from [main] so an integration test can drive it via `testApplication
 * { application { configureServer(graph) } }` against a Testcontainers database.
 */
fun Application.configureServer(graph: ServerGraph) {
    attributes.put(ServerGraphKey, graph)
    graph.pluginInstallers.sortedBy { it.order.ordinal }.forEach { with(it) { install() } }
    graph.routeRegistrars.forEach { with(it) { register() } }
}
