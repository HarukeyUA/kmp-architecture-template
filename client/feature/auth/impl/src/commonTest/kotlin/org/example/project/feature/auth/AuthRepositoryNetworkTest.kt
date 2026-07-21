package org.example.project.feature.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
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
import kotlinx.serialization.serializer
import org.example.project.core.error.CallFailure
import org.example.project.core.network.sessionBearer
import org.example.project.core.secure.storage.ClientSession
import org.example.project.feature.auth.data.AuthRepositoryImpl
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthLoginError
import org.example.project.shared.auth.AuthRefreshError
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.InvalidCredentials
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.auth.TokensResponse
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.commonApiErrorSerializer
import org.example.project.shared.common.encodeApiError
import org.example.project.shared.common.seamJson

/**
 * The client side of the auth gate, against a `MockEngine` server: it exercises the real
 * request-building (shared `@Resource`), the seam `Json` (a 4xx `ErrorEnvelope` parses back to the
 * typed `ApiError`), token-pair storage, and the production [sessionBearer] provider — the native
 * Ktor refresh flow plus the "refresh rejected → clear session" interceptor (ADR-0009 as amended).
 */
class AuthRepositoryNetworkTest {
    private val json = seamJson
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `successful login stores the token pair`() = runTest {
        val (repo, store) =
            fixture {
                respond(
                    json.encodeToString(
                        TokensResponse(
                            accessToken = "login-access",
                            accessTokenExpiresAt = Clock.System.now() + 1.hours,
                            refreshToken = "login-refresh",
                            refreshTokenExpiresAt = Clock.System.now() + 720.hours,
                        )
                    ),
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            }

        val result = repo.login("alice@example.com", "hunter2hunter2")

        assertThat(result.leftOrNull()).isNull()
        assertThat(store.current())
            .isEqualTo(ClientSession(accessToken = "login-access", refreshToken = "login-refresh"))
    }

    @Test
    fun `a rejected login surfaces the declared InvalidCredentials and stores nothing`() = runTest {
        val (repo, store) =
            fixture {
                respond(
                    json.encodeToString(
                        ErrorEnvelope(
                            encodeApiError(serializer<AuthLoginError>(), InvalidCredentials)
                        )
                    ),
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            }

        val result = repo.login("alice@example.com", "wrong-password")

        assertThat(result.leftOrNull()).isEqualTo(CallFailure.Declared(InvalidCredentials))
        assertThat(store.current()).isNull()
    }

    @Test
    fun `a 401 with a live refresh token mints a new access token and retries`() = runTest {
        val store = InMemorySecureSessionStore()
        store.save(ClientSession(accessToken = "expired-access", refreshToken = "refresh-1"))
        val client =
            client(store) { request ->
                when {
                    request.url.encodedPath.endsWith("/auth/refresh") ->
                        respond(
                            json.encodeToString(
                                AccessTokenResponse("minted-access", Clock.System.now() + 1.hours)
                            ),
                            HttpStatusCode.OK,
                            jsonHeaders,
                        )
                    request.headers[HttpHeaders.Authorization] == "Bearer minted-access" ->
                        respond(
                            json.encodeToString(AccountResponse("id", "alice@example.com")),
                            HttpStatusCode.OK,
                            jsonHeaders,
                        )
                    else ->
                        respond(
                            json.encodeToString(
                                ErrorEnvelope(
                                    encodeApiError(commonApiErrorSerializer, Unauthorized)
                                )
                            ),
                            HttpStatusCode.Unauthorized,
                            jsonHeaders,
                        )
                }
            }

        val response = client.get(AuthResource.Me())

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(store.current())
            .isEqualTo(ClientSession(accessToken = "minted-access", refreshToken = "refresh-1"))
    }

    @Test
    fun `a rejected refresh clears the stored session`() = runTest {
        val store = InMemorySecureSessionStore()
        store.save(ClientSession(accessToken = "stale-access", refreshToken = "revoked-refresh"))
        val client =
            client(store) {
                respond(
                    json.encodeToString(
                        ErrorEnvelope(
                            encodeApiError(serializer<AuthRefreshError>(), SessionExpired)
                        )
                    ),
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            }

        // An authed call whose Session the server has revoked: the 401 triggers a refresh attempt,
        // the refresh returns auth.session_expired, and the interceptor clears the local session.
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
            install(Auth) { sessionBearer(store) }
        }
}
