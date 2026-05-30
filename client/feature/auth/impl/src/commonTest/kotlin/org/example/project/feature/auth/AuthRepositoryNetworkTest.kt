package org.example.project.feature.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.example.project.core.error.NetworkError
import org.example.project.core.secure.storage.ClientSession
import org.example.project.feature.auth.data.AuthRepositoryImpl
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.SessionResponse
import org.example.project.shared.auth.authErrorSerializersModule
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.buildSeamJson

/**
 * The client side of the auth gate, against a `MockEngine` server: it exercises the real
 * request-building (shared `@Resource`), the seam `Json` (a 4xx `ErrorEnvelope` parses back to the
 * typed `ApiError`), token storage, and the global 401 → clear-session interceptor (ADR-0009).
 */
class AuthRepositoryNetworkTest {
    private val json = buildSeamJson(setOf(authErrorSerializersModule))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `successful login stores the session token`() = runTest {
        val (repo, store) =
            fixture {
                respond(
                    json.encodeToString(
                        SessionResponse("login-token", Clock.System.now() + 1.hours)
                    ),
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            }

        val result = repo.login("alice@example.com", "hunter2hunter2")

        assertThat(result.leftOrNull()).isNull()
        assertThat(store.current()?.token).isEqualTo("login-token")
    }

    @Test
    fun `a rejected login surfaces the typed error and stores nothing`() = runTest {
        val (repo, store) =
            fixture {
                respond(
                    json.encodeToString(ErrorEnvelope(Unauthorized)),
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            }

        val result = repo.login("alice@example.com", "wrong-password")

        assertThat(result.leftOrNull()).isEqualTo(NetworkError.Api(Unauthorized))
        assertThat(store.current()).isNull()
    }

    @Test
    fun `a 401 on an authenticated request clears the stored session`() = runTest {
        val store = InMemorySecureSessionStore()
        store.save(ClientSession("revoked-token"))
        val client =
            client(store) {
                respond(
                    json.encodeToString(ErrorEnvelope(Unauthorized)),
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            }

        // An authed call whose token the server has revoked.
        client.get(AuthResource.Me())

        assertThat(store.current()).isNull()
    }

    private fun fixture(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): Pair<AuthRepositoryImpl, InMemorySecureSessionStore> {
        val store = InMemorySecureSessionStore()
        return AuthRepositoryImpl(client(store, handler), store) to store
    }

    private fun client(
        store: InMemorySecureSessionStore,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient =
        HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) { json(json) }
            install(Resources)
            install(Auth) {
                bearer {
                    sendWithoutRequest { true }
                    loadTokens {
                        store.current()?.let { BearerTokens(it.token, refreshToken = null) }
                    }
                    refreshTokens {
                        store.clear()
                        null
                    }
                }
            }
        }
}
