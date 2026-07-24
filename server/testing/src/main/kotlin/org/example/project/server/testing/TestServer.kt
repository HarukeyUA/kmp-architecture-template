package org.example.project.server.testing

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.example.project.server.ServerGraph
import org.example.project.server.auth.JwtConfig
import org.example.project.server.configureServer
import org.example.project.server.lifecycle.closeAll
import org.example.project.server.storage.StorageConfig
import org.example.project.server.web.WebLimitsConfig

/**
 * The one way a test boots the `:server:*` stack: a `testApplication` block with every
 * self-registered plugin + route from a freshly built [ServerGraph], handing [block] the
 * seam-framed [HttpClient]. Each call starts from a clean slate — the shared [TestPostgres] schema
 * is reset and a fresh graph gives each test its own session cache.
 *
 * The graph's own HikariCP pool (opened by anything that resolves the `DataSource`) is released
 * here in a `finally`; suites never see the graph, so the accumulated-pools footgun (each leaked
 * pool holds connections against the one shared container until Postgres' limit corrupts sibling
 * suites) is unrepresentable rather than a per-suite discipline.
 *
 * [storageConfig] defaults to a throwaway config: the S3 client is provided lazily, so tests that
 * don't touch the bucket leg never construct it; a blob-exercising test passes
 * [TestMinio.storageConfig]. [webLimitsConfig] / [jwtConfig] default to roomy production-shaped
 * values — a test probing the limits themselves passes tight ones (see
 * `WebHardeningIntegrationTest`).
 *
 * [block]'s receiver is the raw [ApplicationTestBuilder] for harnesses that build bespoke clients
 * on top.
 */
fun serverTest(
    storageConfig: StorageConfig = testStorageConfig(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
    block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
) = serverGraphTest(storageConfig, webLimitsConfig, jwtConfig) { http, _ -> block(http) }

/**
 * [serverTest] for the rare suite that must reach a real service on the booted [ServerGraph]
 * directly (e.g. `SessionStore.revokeAllFor`, which has no route). Prefer [serverTest]: driving
 * through the seam client is what pins the wire.
 */
fun serverGraphTest(
    storageConfig: StorageConfig = testStorageConfig(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
    block: suspend ApplicationTestBuilder.(HttpClient, ServerGraph) -> Unit,
) = testApplication {
    val graph = installTestServer(storageConfig, webLimitsConfig, jwtConfig)
    try {
        block(seamClient(), graph)
    } finally {
        graph.serverResources.closeAll()
    }
}

/**
 * Installs a freshly built graph's plugins + routes into this `testApplication` after rebinding the
 * shared [TestPostgres] as the ambient Exposed default and resetting its schema; the graph's
 * request-serving services run against that ambient default, not the graph's own pool, and the
 * shared pool is already migrated — `databaseBootstrap.start()` is deliberately not called, so no
 * per-test Flyway pass runs.
 *
 * Internal on purpose: the returned graph may own a HikariCP pool, and only [serverTest] is trusted
 * with the matching `closeAll()`. Suites that need a graph without the shared container use
 * [buildTestGraph].
 */
internal fun ApplicationTestBuilder.installTestServer(
    storageConfig: StorageConfig = testStorageConfig(),
    webLimitsConfig: WebLimitsConfig = testWebLimitsConfig(),
    jwtConfig: JwtConfig = testJwtConfig(),
): ServerGraph {
    TestPostgres.connect()
    TestPostgres.resetSchema()
    val graph =
        buildTestGraph(TestPostgres.databaseConfig(), storageConfig, webLimitsConfig, jwtConfig)
    application { configureServer(graph) }
    return graph
}

/**
 * The seam [Json] the harness clients speak — the same static instance production uses on both
 * ends, so success bodies decode with identical framing and any 4xx `ErrorEnvelope` narrows through
 * the same sealed lenses `:server:app` encodes with.
 */
val seamJson: Json = org.example.project.shared.common.seamJson

fun ApplicationTestBuilder.seamClient(): HttpClient = createClient {
    install(ContentNegotiation) { json(seamJson) }
    install(Resources)
}
