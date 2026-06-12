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
import java.util.UUID
import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.example.project.server.auth.AccountId
import org.example.project.server.database.DatabaseConfig
import org.example.project.server.database.dbTransaction
import org.example.project.server.observability.MetricsConfig
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.authErrorSerializersModule
import org.example.project.shared.common.BadRequest
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.Unauthorized
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
            // Object storage is unused here; the lazy S3 client is never built, so any value works.
            val storageConfig = testStorageConfig()
            val metricsConfig = MetricsConfig(port = 0)
            val webLimitsConfig = testWebLimitsConfig()
            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(
                        ServerConfig(
                            "localhost",
                            port = 0,
                            version = "test",
                            databaseConfig,
                            storageConfig,
                            metricsConfig,
                            webLimitsConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
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
                val storedSessionKeys = dbTransaction {
                    val keys = mutableListOf<String>()
                    exec("SELECT token_hash FROM sessions") { rs ->
                        while (rs.next()) {
                            keys += rs.getString("token_hash")
                        }
                    }
                    keys
                }
                assertThat(storedSessionKeys.size).isEqualTo(1)
                assertThat(storedSessionKeys.single() == token).isEqualTo(false)

                // Authenticated call → 200 + the Principal's account.
                val me = client.get(AuthResource.Me()) { bearerAuth(token) }
                assertThat(me.status).isEqualTo(HttpStatusCode.OK)
                assertThat(me.body<AccountResponse>().email).isEqualTo("alice@example.com")

                // No token → 401.
                val missingToken = client.get(AuthResource.Me())
                assertThat(missingToken.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(missingToken.body<ErrorEnvelope>().error).isEqualTo(Unauthorized)

                // Log in with the same credentials → 200 (a second valid session).
                val login =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("alice@example.com", "hunter2hunter2"))
                    }
                assertThat(login.status).isEqualTo(HttpStatusCode.OK)

                // Wrong password → the service raises Unauthorized, so the route already envelopes
                // the 401 (unlike the body-less auth *challenge* above). The StatusPages 401 hook
                // must leave this enveloped response alone rather than rebuilding/clobbering it.
                val wrongPassword =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("alice@example.com", "wrongpassword99"))
                    }
                assertThat(wrongPassword.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(wrongPassword.body<ErrorEnvelope>().error).isEqualTo(Unauthorized)

                // Unknown email → byte-identical envelope to wrong-password: the collapse to
                // Unauthorized is the information-disclosure boundary, and the service burns a
                // dummy Argon2 verify on this path so timing doesn't pierce it either (the
                // dummy-verify contract itself is pinned in DefaultAuthServiceTest).
                val unknownEmail =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("nobody@example.com", "hunter2hunter2"))
                    }
                assertThat(unknownEmail.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(unknownEmail.body<ErrorEnvelope>().error).isEqualTo(Unauthorized)

                // Log out (server-side revoke) → 204.
                assertThat(client.post(AuthResource.Logout()) { bearerAuth(token) }.status)
                    .isEqualTo(HttpStatusCode.NoContent)

                // The revoked token now resolves to nothing → 401.
                val revoked = client.get(AuthResource.Me()) { bearerAuth(token) }
                assertThat(revoked.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(revoked.body<ErrorEnvelope>().error).isEqualTo(Unauthorized)

                // Signing up the same email again → the typed domain error round-trips back.
                val duplicate =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(duplicate.status).isEqualTo(HttpStatusCode.Conflict)
                assertThat(duplicate.body<ErrorEnvelope>().error).isEqualTo(EmailTaken)

                // Malformed JSON is a typed 400, not a leaked 500 from the catch-all StatusPages
                // hook.
                val malformed =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":""")
                    }
                assertThat(malformed.status).isEqualTo(HttpStatusCode.BadRequest)
                assertThat(malformed.body<ErrorEnvelope>().error)
                    .isEqualTo(BadRequest("malformed_body"))
            }
        }
    }

    @Test
    fun `revokeAllFor kills every session for the account immediately, including cached ones`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val databaseConfig =
                DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
            val storageConfig = testStorageConfig()
            val metricsConfig = MetricsConfig(port = 0)
            val webLimitsConfig = testWebLimitsConfig()
            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(
                        ServerConfig(
                            "localhost",
                            port = 0,
                            version = "test",
                            databaseConfig,
                            storageConfig,
                            metricsConfig,
                            webLimitsConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
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
                val credentials = SignupRequest("bob@example.com", "hunter2hunter2")

                // Two independent sessions for the same account: signup + a separate login.
                val signup =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(signup.status).isEqualTo(HttpStatusCode.Created)
                val firstToken = signup.body<SessionResponse>().token
                val login =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("bob@example.com", "hunter2hunter2"))
                    }
                assertThat(login.status).isEqualTo(HttpStatusCode.OK)
                val secondToken = login.body<SessionResponse>().token

                // Resolve both so they are *cached* — revoke-all must defeat the cache, not just
                // the table.
                val me = client.get(AuthResource.Me()) { bearerAuth(firstToken) }
                assertThat(me.status).isEqualTo(HttpStatusCode.OK)
                val accountId = AccountId(UUID.fromString(me.body<AccountResponse>().id))
                assertThat(client.get(AuthResource.Me()) { bearerAuth(secondToken) }.status)
                    .isEqualTo(HttpStatusCode.OK)

                graph.sessionStore.revokeAllFor(accountId)

                // Both sessions 401 immediately on this node — no TTL wait.
                for (token in listOf(firstToken, secondToken)) {
                    val revoked = client.get(AuthResource.Me()) { bearerAuth(token) }
                    assertThat(revoked.status).isEqualTo(HttpStatusCode.Unauthorized)
                    assertThat(revoked.body<ErrorEnvelope>().error).isEqualTo(Unauthorized)
                }

                // A fresh login afterwards works — revocation is not a lockout.
                val relogin =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("bob@example.com", "hunter2hunter2"))
                    }
                assertThat(relogin.status).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `concurrent signup of the same email yields one account and one typed conflict`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val databaseConfig =
                DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
            val storageConfig = testStorageConfig()
            val metricsConfig = MetricsConfig(port = 0)
            val webLimitsConfig = testWebLimitsConfig()
            val graph =
                createGraphFactory<ServerGraph.Factory>()
                    .create(
                        ServerConfig(
                            "localhost",
                            port = 0,
                            version = "test",
                            databaseConfig,
                            storageConfig,
                            metricsConfig,
                            webLimitsConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
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
                val credentials = SignupRequest("race@example.com", "hunter2hunter2")

                val responses = coroutineScope {
                    val first = async {
                        client.post(AuthResource.Signup()) {
                            contentType(ContentType.Application.Json)
                            setBody(credentials)
                        }
                    }
                    val second = async {
                        client.post(AuthResource.Signup()) {
                            contentType(ContentType.Application.Json)
                            setBody(credentials)
                        }
                    }
                    listOf(first.await(), second.await())
                }

                val statuses = responses.map { it.status }.sortedBy { it.value }
                assertThat(statuses)
                    .isEqualTo(listOf(HttpStatusCode.Created, HttpStatusCode.Conflict))
                val conflict = responses.single { it.status == HttpStatusCode.Conflict }
                assertThat(conflict.body<ErrorEnvelope>().error).isEqualTo(EmailTaken)
            }
        }
    }
}
