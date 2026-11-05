package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.example.project.server.lifecycle.ServerEnvironment
import org.example.project.server.lifecycle.closeAll
import org.example.project.server.testing.TestPostgres

/**
 * Object storage is opt-in: a server with no `S3_*` environment and no `BlobStore` consumer takes
 * the path `main` takes, from loading config through serving to releasing its resources.
 */
class BootWithoutStorageTest {
    @Test
    fun `server with no S3 environment boots, serves and shuts down`() {
        val database = TestPostgres.databaseConfig()
        val env =
            mapOf(
                "DATABASE_URL" to database.jdbcUrl,
                "DATABASE_USER" to database.username.orEmpty(),
                "DATABASE_PASSWORD" to database.password.orEmpty(),
                "JWT_SECRET" to "test-only-jwt-secret-0123456789abcdef",
            )
        val environment = ServerEnvironment { key ->
            check(!key.startsWith("S3_")) { "Boot read '$key'" }
            env[key]
        }

        val graph = createServerGraph(ServerConfig.load(environment), environment)
        graph.databaseBootstrap.start()
        testApplication {
            application { configureServer(graph) }

            assertThat(client.get("/health").status).isEqualTo(HttpStatusCode.OK)
        }
        runBlocking { graph.serverResources.closeAll() }
    }
}
