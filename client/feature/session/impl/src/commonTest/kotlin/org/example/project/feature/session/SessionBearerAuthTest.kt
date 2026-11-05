package org.example.project.feature.session

import assertk.assertThat
import assertk.assertions.isEqualTo
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
import kotlinx.coroutines.test.runTest
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.encodeApiError
import org.example.project.shared.common.seamJson

/** A bearer 401 is routine token expiry; the refresh endpoint's verdict is the only sign-out. */
class SessionBearerAuthTest {

    @Test
    fun expiredAccessToken_isRefreshedAndTheRequestRetriedWithTheMintedToken() = runTest {
        val store = FakeSessionRepository(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.Mints(MINTED_ACCESS))

        val response = client.get("http://test/v1/resource")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(store.current)
            .isEqualTo(SessionState.Bound(ClientSession(MINTED_ACCESS, REFRESH)))
    }

    @Test
    fun sessionExpiredFromTheRefreshEndpoint_clearsTheStoredSession() = runTest {
        val store = FakeSessionRepository(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.SessionExpired)

        val response = client.get("http://test/v1/resource")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(store.current).isEqualTo(SessionState.Unbound)
    }

    @Test
    fun ambientRefreshFailure_leavesTheSessionInPlace() = runTest {
        val store = FakeSessionRepository(ClientSession(EXPIRED_ACCESS, REFRESH))
        val client = client(store, refreshResponse = RefreshResponse.ServerError)

        val response = client.get("http://test/v1/resource")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(store.current)
            .isEqualTo(SessionState.Bound(ClientSession(EXPIRED_ACCESS, REFRESH)))
    }

    @Test
    fun failedSessionRead_sendsNoBearerAndLeavesTheSessionInPlace() = runTest {
        val store = FakeSessionRepository(ClientSession(EXPIRED_ACCESS, REFRESH))
        store.nextSnapshotError = SessionRepositoryError.Storage(IllegalStateException("io"))
        val client = client(store, refreshResponse = RefreshResponse.SessionExpired)

        val response = client.get("http://test/v1/resource")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(store.clearCalls).isEqualTo(0)
    }

    @Test
    fun configurer_installsTheBearerProvider() = runTest {
        val store = FakeSessionRepository(ClientSession(MINTED_ACCESS, REFRESH))
        val client =
            HttpClient(
                MockEngine { request ->
                    assertThat(request.headers[HttpHeaders.Authorization])
                        .isEqualTo("Bearer $MINTED_ACCESS")
                    respond(content = "", status = HttpStatusCode.OK)
                },
            ) {
                SessionBearerConfigurer(store).configure(this)
            }

        assertThat(client.get("http://test/v1/resource").status).isEqualTo(HttpStatusCode.OK)
    }

    // The mock's protected route accepts only MINTED_ACCESS. Its 401 challenge carries
    // `WWW-Authenticate: Bearer`, as the real JWT guard does, since that arms Ktor's refresh hook.
    private fun client(store: SessionRepository, refreshResponse: RefreshResponse): HttpClient {
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
                                            ),
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

    private companion object {
        const val EXPIRED_ACCESS = "expired-access-token"
        const val MINTED_ACCESS = "minted-access-token"
        const val REFRESH = "refresh-token"
    }
}
