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
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.InvalidCredentials
import org.example.project.shared.auth.LoginRequest
import org.example.project.shared.auth.LogoutRequest
import org.example.project.shared.auth.RefreshRequest
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.auth.SignupRequest
import org.example.project.shared.auth.TokensResponse
import org.example.project.shared.common.BadRequest
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.seamJson
import org.testcontainers.containers.PostgreSQLContainer

/**
 * The Phase 4 validation gate, server-side (ADR-0002/0009): the whole stack — seam, contract, shape
 * validation, error model, auth, persistence, DI — driven through the real routes against a
 * Testcontainers Postgres. The test client consumes the **same** `@Resource` and seam `Json` as the
 * client app, so this also proves Ktor Resources shared across both ends.
 */
class AuthIntegrationTest {
    @Test
    fun `signup, authenticated call, login, refresh, then revoke kills the refresh path`() {
        PostgreSQLContainer("postgres:17-alpine").use { postgres ->
            postgres.start()
            val databaseConfig =
                DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
            // Object storage is unused here; the lazy S3 client is never built, so any value works.
            val storageConfig = testStorageConfig()
            val metricsConfig = MetricsConfig(port = 0)
            val webLimitsConfig = testWebLimitsConfig()
            val jwtConfig = testJwtConfig()
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
                            jwtConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
                        jwtConfig,
                    )
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(seamJson)
                    }
                    install(Resources)
                }
                val credentials = SignupRequest("alice@example.com", "hunter2hunter2")

                // Sign up → 201 + the token pair: a JWT access token and an opaque refresh token.
                val signup =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(signup.status).isEqualTo(HttpStatusCode.Created)
                val tokens = signup.body<TokensResponse>()
                // Only the refresh token has server-side state, and it's stored hashed — the raw
                // token never touches the table, and the stateless access token has no row at all.
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
                assertThat(storedSessionKeys.single() == tokens.refreshToken).isEqualTo(false)

                // Authenticated call with the access token → 200 + the Principal's account. The
                // JWT is verified statelessly — no sessions lookup on this path.
                val me = client.get(AuthResource.Me()) { bearerAuth(tokens.accessToken) }
                assertThat(me.status).isEqualTo(HttpStatusCode.OK)
                assertThat(me.body<AccountResponse>().email).isEqualTo("alice@example.com")

                // No token → 401.
                val missingToken = client.get(AuthResource.Me())
                assertThat(missingToken.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(missingToken.decodedError()).isEqualTo(Unauthorized)

                // A tampered JWT (signature no longer matches) → 401, rejected by verification
                // alone.
                val tampered =
                    client.get(AuthResource.Me()) { bearerAuth(tokens.accessToken.dropLast(2)) }
                assertThat(tampered.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(tampered.decodedError()).isEqualTo(Unauthorized)

                // The refresh token is not an access token: it means nothing to the JWT provider.
                val refreshAsBearer =
                    client.get(AuthResource.Me()) { bearerAuth(tokens.refreshToken) }
                assertThat(refreshAsBearer.status).isEqualTo(HttpStatusCode.Unauthorized)

                // Log in with the same credentials → 200 (a second valid session).
                val login =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("alice@example.com", "hunter2hunter2"))
                    }
                assertThat(login.status).isEqualTo(HttpStatusCode.OK)

                // Wrong password → the service declares InvalidCredentials, so the route already
                // envelopes the 401 (unlike the body-less auth *challenge* above, which stays a
                // cross-cutting Unauthorized). The StatusPages 401 hook must leave this enveloped
                // response alone rather than rebuilding/clobbering it.
                val wrongPassword =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("alice@example.com", "wrongpassword99"))
                    }
                assertThat(wrongPassword.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(wrongPassword.decodedError()).isEqualTo(InvalidCredentials)

                // Unknown email → byte-identical envelope to wrong-password: the collapse to the
                // single InvalidCredentials is the information-disclosure boundary (ADR-0011), and
                // the service burns a dummy Argon2 verify on this path so timing doesn't pierce it
                // either (the dummy-verify contract itself is pinned in DefaultAuthServiceTest).
                val unknownEmail =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("nobody@example.com", "hunter2hunter2"))
                    }
                assertThat(unknownEmail.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(unknownEmail.decodedError()).isEqualTo(InvalidCredentials)

                // Refresh: the opaque token mints a fresh access token (the one place the session
                // store is consulted per request cycle); the minted JWT works immediately.
                val refreshed =
                    client.post(AuthResource.Refresh()) {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(tokens.refreshToken))
                    }
                assertThat(refreshed.status).isEqualTo(HttpStatusCode.OK)
                val mintedAccessToken = refreshed.body<AccessTokenResponse>().accessToken
                assertThat(client.get(AuthResource.Me()) { bearerAuth(mintedAccessToken) }.status)
                    .isEqualTo(HttpStatusCode.OK)

                // A garbage refresh token → the Declared SessionExpired (refresh presents a
                // Session, not an Access token, so it never reuses the cross-cutting Unauthorized).
                val badRefresh =
                    client.post(AuthResource.Refresh()) {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest("not-a-real-refresh-token"))
                    }
                assertThat(badRefresh.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(badRefresh.decodedError()).isEqualTo(SessionExpired)

                // Log out (revoke the refresh token server-side) → 204.
                val logout =
                    client.post(AuthResource.Logout()) {
                        bearerAuth(tokens.accessToken)
                        contentType(ContentType.Application.Json)
                        setBody(LogoutRequest(tokens.refreshToken))
                    }
                assertThat(logout.status).isEqualTo(HttpStatusCode.NoContent)

                // The revoked refresh token mints nothing → 401 SessionExpired …
                val revokedRefresh =
                    client.post(AuthResource.Refresh()) {
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(tokens.refreshToken))
                    }
                assertThat(revokedRefresh.status).isEqualTo(HttpStatusCode.Unauthorized)
                assertThat(revokedRefresh.decodedError()).isEqualTo(SessionExpired)

                // … while the already-minted access token keeps working until its TTL runs out —
                // the bounded revocation staleness the JWT amendment to ADR-0009 accepts by design.
                assertThat(client.get(AuthResource.Me()) { bearerAuth(tokens.accessToken) }.status)
                    .isEqualTo(HttpStatusCode.OK)

                // Signing up the same email again → the typed domain error round-trips back.
                val duplicate =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody(credentials)
                    }
                assertThat(duplicate.status).isEqualTo(HttpStatusCode.Conflict)
                assertThat(duplicate.decodedError()).isEqualTo(EmailTaken)

                // Malformed JSON is a typed 400, not a leaked 500 from the catch-all StatusPages
                // hook.
                val malformed =
                    client.post(AuthResource.Signup()) {
                        contentType(ContentType.Application.Json)
                        setBody("""{"email":""")
                    }
                assertThat(malformed.status).isEqualTo(HttpStatusCode.BadRequest)
                assertThat(malformed.decodedError()).isEqualTo(BadRequest("malformed_body"))
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
            val jwtConfig = testJwtConfig()
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
                            jwtConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
                        jwtConfig,
                    )
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(seamJson)
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
                val firstRefreshToken = signup.body<TokensResponse>().refreshToken
                val login =
                    client.post(AuthResource.Login()) {
                        contentType(ContentType.Application.Json)
                        setBody(LoginRequest("bob@example.com", "hunter2hunter2"))
                    }
                assertThat(login.status).isEqualTo(HttpStatusCode.OK)
                val secondTokens = login.body<TokensResponse>()

                // Refresh with both so the sessions are *cached* — revoke-all must defeat the
                // cache, not just the table. (Access tokens never touch the session store; only
                // the refresh path does.)
                for (refreshToken in listOf(firstRefreshToken, secondTokens.refreshToken)) {
                    val refreshed =
                        client.post(AuthResource.Refresh()) {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(refreshToken))
                        }
                    assertThat(refreshed.status).isEqualTo(HttpStatusCode.OK)
                }
                val me = client.get(AuthResource.Me()) { bearerAuth(secondTokens.accessToken) }
                assertThat(me.status).isEqualTo(HttpStatusCode.OK)
                val accountId = AccountId(UUID.fromString(me.body<AccountResponse>().id))

                graph.sessionStore.revokeAllFor(accountId)

                // Both sessions stop minting immediately on this node — no TTL wait.
                for (refreshToken in listOf(firstRefreshToken, secondTokens.refreshToken)) {
                    val revoked =
                        client.post(AuthResource.Refresh()) {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(refreshToken))
                        }
                    assertThat(revoked.status).isEqualTo(HttpStatusCode.Unauthorized)
                    assertThat(revoked.decodedError()).isEqualTo(SessionExpired)
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
            val jwtConfig = testJwtConfig()
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
                            jwtConfig,
                        ),
                        databaseConfig,
                        storageConfig,
                        metricsConfig,
                        webLimitsConfig,
                        jwtConfig,
                    )
            graph.databaseBootstrap.start()

            testApplication {
                application { configureServer(graph) }
                val client = createClient {
                    install(ContentNegotiation) {
                        json(seamJson)
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
                assertThat(conflict.decodedError()).isEqualTo(EmailTaken)
            }
        }
    }
}
