package org.example.project.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.encodeApiError
import org.example.project.shared.common.seamJson

/**
 * Pins [sessionBearer]'s 401 coordination — the template's whole rejected-credential story
 * (ADR-0009 as amended). Witchy-notes handles this with a receive-pipeline reporter because its
 * device tokens have no refresh path (any bearer 401 is final there); here a bearer 401 is routine
 * token expiry, and the *refresh endpoint's* verdict is the only credential signal:
 * - refresh mints → the original request retries and succeeds, session rotates in place;
 * - refresh declares `auth.session_expired` → the local session is purged (root nav → Login);
 * - refresh fails ambiently (5xx, network) → the session survives; a flaky network or a sleeping
 *   PaaS instance must never log the user out.
 */
class SessionBearerAuthTest {

    @Test
    fun expiredAccessToken_isRefreshedAndTheRequestRetriedWithTheMintedToken() = runTest {
        val store = InMemorySessionStore(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.Mints(MINTED_ACCESS))

        val response = client.get("http://test/v1/notes")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(store.current()).isEqualTo(ClientSession(MINTED_ACCESS, REFRESH))
    }

    @Test
    fun sessionExpiredFromTheRefreshEndpoint_clearsTheStoredSession() = runTest {
        val store = InMemorySessionStore(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.SessionExpired)

        val response = client.get("http://test/v1/notes")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(store.current()).isNull()
    }

    @Test
    fun ambientRefreshFailure_leavesTheSessionInPlace() = runTest {
        val store = InMemorySessionStore(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.ServerError)

        val response = client.get("http://test/v1/notes")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(store.current()).isNotNull()
    }

    /**
     * A mock server whose protected route accepts only [MINTED_ACCESS] and whose refresh endpoint
     * behaves per [refreshResponse]. The 401 challenge carries `WWW-Authenticate: Bearer`, matching
     * the real server's JWT guard, which is what arms Ktor's bearer refresh hook.
     */
    private fun client(store: SecureSessionStore, refreshResponse: RefreshResponse): HttpClient {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/v1/auth/refresh" ->
                    when (refreshResponse) {
                        is RefreshResponse.Mints ->
                            respond(
                                content =
                                    seamJson.encodeToString(
                                        AccessTokenResponse.serializer(),
                                        AccessTokenResponse(
                                            accessToken = refreshResponse.accessToken,
                                            expiresAt = Instant.fromEpochSeconds(4_000_000_000),
                                        ),
                                    ),
                                status = HttpStatusCode.OK,
                                headers = jsonHeaders(),
                            )
                        RefreshResponse.SessionExpired ->
                            respond(
                                content =
                                    seamJson.encodeToString(
                                        ErrorEnvelope.serializer(),
                                        ErrorEnvelope(
                                            encodeApiError(
                                                checkNotNull(AuthApi.refresh.error),
                                                SessionExpired,
                                            )
                                        ),
                                    ),
                                status = HttpStatusCode.Unauthorized,
                                headers = jsonHeaders(),
                            )
                        RefreshResponse.ServerError ->
                            respond(content = "", status = HttpStatusCode.InternalServerError)
                    }

                request.headers[HttpHeaders.Authorization] == "Bearer $MINTED_ACCESS" ->
                    respond(content = "", status = HttpStatusCode.OK)

                else ->
                    respond(
                        content = "",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.WWWAuthenticate, "Bearer"),
                    )
            }
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(seamJson) }
            install(Resources)
            install(Auth) { sessionBearer(store) }
        }
    }

    private fun jsonHeaders() =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private sealed interface RefreshResponse {
        data class Mints(val accessToken: String) : RefreshResponse

        data object SessionExpired : RefreshResponse

        data object ServerError : RefreshResponse
    }

    private class InMemorySessionStore(initial: ClientSession?) : SecureSessionStore {
        private val state = MutableStateFlow(initial)

        override fun sessionFlow(): Flow<ClientSession?> = state

        override suspend fun current(): ClientSession? = state.value

        override suspend fun save(session: ClientSession) {
            state.value = session
        }

        override suspend fun clear() {
            state.value = null
        }
    }

    private companion object {
        const val EXPIRED_ACCESS = "expired-access-token"
        const val MINTED_ACCESS = "minted-access-token"
        const val REFRESH = "refresh-token"
    }
}
