package org.example.project.server

import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import dev.zacsweers.metro.createGraphFactory
import io.ktor.server.application.Application
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import org.example.project.server.lifecycle.closeAll

/**
 * The live [ServerGraph], stashed on the application so integration tests can reach real services.
 */
val ServerGraphKey: AttributeKey<ServerGraph> = AttributeKey("ServerGraph")

suspend fun main(): Unit = resourceScope {
    val config = ServerConfig.load()
    val graph = installServerGraph(config)
    graph.databaseBootstrap.start()
    embeddedServer(
            Netty,
            configure = {
                // Public application port (mapped to the public domain/ingress).
                connector {
                    host = config.host
                    port = config.port
                }
                // Metrics port — kept off the public surface; only an in-network Prometheus reaches
                // it. `MetricsRoute` 404s `/metrics` requests that arrive on any other connector.
                connector {
                    host = config.host
                    port = config.metrics.port
                }
            },
            module = { configureServer(graph) },
        )
        .start(wait = true)
}

private suspend fun ResourceScope.installServerGraph(config: ServerConfig): ServerGraph =
    install({
        createGraphFactory<ServerGraph.Factory>()
            .create(
                config,
                config.database,
                config.storage,
                config.metrics,
                config.webLimits,
                config.jwt,
            )
    }) { graph, _ ->
        graph.serverResources.closeAll()
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
