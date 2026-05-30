package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.zacsweers.metro.createGraphFactory
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import org.example.project.server.database.DatabaseConfig
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.authErrorSerializersModule
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.buildSeamJson
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The Phase 4 validation gate, server-side (ADR-0002/0009): the whole stack — seam, contract, shape
 * validation, error model, auth, persistence, DI — driven through the real routes against a
 * Testcontainers Postgres. The test client consumes the **same** `@Resource` and seam `Json` as the
 * client app, so this also proves Ktor Resources shared across both ends.
 */
class AuthIntegrationTest {
    @Test
    fun `signup, authenticated call, login, then revoke yields 401`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val databaseConfig =
                DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(
                        ServerConfig("localhost", port = 0, version = "test", databaseConfig),
                        databaseConfig,
                    )
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(buildSeamJson(setOf(authErrorSerializersModule)))
                    }
                    install(Resources)
                }
                val credentials = SignupRequest("alice@example.com", "hunter2hunter2")

                // Sign up → 201 + an opaque session token.
                val signup =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(signup.status).isEqualTo(HttpStatusCode.Created)
                val token = signup.body<SessionResponse>().token

                // Authenticated call → 200 + the Principal's account.
                val me = client.get(AuthResource.Me()) { bearerAuth(token) }
                assertThat(me.status).isEqualTo(HttpStatusCode.OK)
                assertThat(me.body<AccountResponse>().email).isEqualTo("alice@example.com")

                // No token → 401.
                assertThat(client.get(AuthResource.Me()).status)
                    .isEqualTo(HttpStatusCode.Unauthorized)

                // Log in with the same credentials → 200 (a second valid session).
                val login =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("alice@example.com", "hunter2hunter2"))
                    }
                assertThat(login.status).isEqualTo(HttpStatusCode.OK)

                // Log out (server-side revoke) → 204.
                assertThat(client.post(AuthResource.Logout()) { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.NoContent)

                // The revoked token now resolves to nothing → 401.
                assertThat(client.get(AuthResource.Me()) { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.Unauthorized)

                // Signing up the same email again → the typed domain error round-trips back.
                val duplicate =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(duplicate.body<ErrorEnvelope>().error).isEqualTo(EmailTaken)
            }
        }
    }
}
