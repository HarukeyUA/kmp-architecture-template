package org.example.project.server

import dev.zacsweers.metro.createGraphFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.closeAll

/**
 * The live [ServerGraph], stashed on the application so integration tests can reach real services.
 */
val ServerGraphKey: AttributeKey<ServerGraph> = AttributeKey("ServerGraph")

// How long stop lets in-flight calls finish before it starts closing connections.
private const val SHUTDOWN_GRACE_MILLIS: Long = 3_000

// Hard ceiling on the whole engine stop, grace included.
private const val SHUTDOWN_TIMEOUT_MILLIS: Long = 10_000

// Seconds a connection may go without delivering a byte before it is closed. Netty's default is
// infinite, which lets a client hold a socket open for free.
private const val REQUEST_READ_TIMEOUT_SECONDS: Int = 120

fun main() {
    val config = ServerConfig.load()
    val graph = createServerGraph(config)
    graph.databaseBootstrap.start()
    val server =
        embeddedServer(
            Netty,
            configure = {
                shutdownGracePeriod = SHUTDOWN_GRACE_MILLIS
                shutdownTimeout = SHUTDOWN_TIMEOUT_MILLIS
                requestReadTimeoutSeconds = REQUEST_READ_TIMEOUT_SECONDS
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
    // On SIGTERM Ktor's own shutdown hook calls stop(), which raises this event before the hook
    // returns, so the graph is released while the JVM is still waiting on that hook.
    server.monitor.subscribe(ApplicationStopped) {
        runBlocking { graph.serverResources.closeAll() }
    }
    server.start(wait = true)
}

/**
 * Assembles the [ServerGraph] from [config], threading the sub-configs like the graph factory
 * expects. Owns no lifecycle, so the integration-test harness can build the real graph itself and
 * own its teardown.
 */
fun createServerGraph(
    config: ServerConfig,
    environment: ServerEnvironment = ServerEnvironment.Process,
): ServerGraph =
    createGraphFactory<ServerGraph.Factory>()
        .create(config, config.database, config.metrics, config.webLimits, config.jwt, environment)

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
