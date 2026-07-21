package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.observability.MetricsConfig
import org.example.project.server.web.WebLimitsConfig
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.common.PayloadTooLarge
import org.example.project.shared.common.RateLimited
import org.example.project.shared.common.seamJson
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The app-layer DoS hardening (ADR-0010 §11) driven through the real stack: the strict per-IP tier
 * on credential endpoints answers with the typed `RateLimited` envelope (never Ktor's bare 429),
 * non-credential endpoints are deliberately unlimited, and an over-limit body is a typed
 * `PayloadTooLarge` 413 — not the 400 "malformed_body" it would fall into without the explicit
 * StatusPages handler.
 */
class WebHardeningIntegrationTest {
    @Test
    fun `credential endpoints rate-limit with a typed envelope, others stay unlimited`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val graph = buildGraph(postgres, testWebLimitsConfig(credentialRateLimit = 2))
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(seamJson)
                    }
                    install(Resources)
                }

                // Two logins consume the budget (their 401s are irrelevant — the limiter counts
                // requests, not successes).
                repeat(2) {
                    val login =
                        client.post(AuthResource.Login()) {
                            contentType(ContentType.Application.Json)
                            setBody(LoginRequest("ghost@example.com", "hunter2hunter2"))
                        }
                    assertThat(login.status).isEqualTo(HttpStatusCode.Unauthorized)
                }

                // The third is shed before the handler: typed RateLimited envelope with the
                // Retry-After header lifted into the actionable field.
                val limited =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("ghost@example.com", "hunter2hunter2"))
                    }
                assertThat(limited.status).isEqualTo(HttpStatusCode.TooManyRequests)
                val error = limited.decodedError()
                assertThat(error).isInstanceOf(RateLimited::class)
                assertThat((error as RateLimited).retryAfterSeconds).isNotNull()

                // Strict-only posture: non-credential endpoints have no tier at all. The body is
                // also pinned status-only: per-check names/details would hand a public prober the
                // infra composition, so they go to the log instead (HealthRoute).
                repeat(4) {
                    val health = client.get("/health")
                    assertThat(health.status).isEqualTo(HttpStatusCode.OK)
                    val body = Json.parseToJsonElement(health.bodyAsText()).jsonObject
                    assertThat(body.keys).isEqualTo(setOf("status"))
                    assertThat(body["status"]?.jsonPrimitive?.content).isEqualTo("UP")
                }
            }
        }
    }

    @Test
    fun `over-limit body is a typed 413, within-limit bodies still work`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val graph = buildGraph(postgres, testWebLimitsConfig(maxRequestBodyBytes = 512))
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(seamJson)
                    }
                    install(Resources)
                }

                // Well-formed JSON over the cap: 413 PayloadTooLarge, NOT 400 malformed_body —
                // the body never reaches the parser.
                val oversized =
                    client.post("/v1/auth/signup") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":"big@example.com","password":"${"x".repeat(2_000)}"}""")
                    }
                assertThat(oversized.status).isEqualTo(HttpStatusCode.PayloadTooLarge)
                assertThat(oversized.decodedError()).isEqualTo(PayloadTooLarge)

                // The cap doesn't break the happy path.
                val signup =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(SignupRequest("ok@example.com", "hunter2hunter2"))
                    }
                assertThat(signup.status).isEqualTo(HttpStatusCode.Created)
            }
        }
    }

    private fun buildGraph(
        postgres: PostgreSQLContainer<*>,
        webLimitsConfig: WebLimitsConfig,
    ): ServerGraph {
        val databaseConfig = DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
        val storageConfig = testStorageConfig()
        val metricsConfig = MetricsConfig(port = 0)
        val jwtConfig = testJwtConfig()
        return createGraphFactory<ServerGraph.Factory>()
            .create(
                ServerConfig(
                    "localhost",
                    port = 0,
                    version = "test",
                    databaseConfig,
                    storageConfig,
                    metricsConfig,
                    webLimitsConfig,
                    jwtConfig,
                ),
                databaseConfig,
                storageConfig,
                metricsConfig,
                webLimitsConfig,
                jwtConfig,
            )
    }
}
