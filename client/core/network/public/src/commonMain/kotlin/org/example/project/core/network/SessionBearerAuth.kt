package org.example.project.core.network

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.RefreshRequest

/**
 * The app's bearer provider (ADR-0009 as amended): attaches the stored access token to every
 * request, and on a 401 uses Ktor's native refresh hook to mint a replacement through `POST
 * /v1/auth/refresh` (the refresh call itself is marked so the plugin doesn't intercept it, and the
 * original request is retried once with the new token).
 *
 * A 401 from the *refresh endpoint* means the session is revoked or expired server-side — the
 * global "401 → clear session" interceptor: clearing makes [SecureSessionStore.sessionFlow] emit
 * null, which the root navigation observes and bounces to Login. Refresh failures that aren't a 401
 * (5xx; connection errors propagate as exceptions into the caller's typed [NetworkError]) leave the
 * session in place — a flaky network must not log the user out.
 */
fun AuthConfig.sessionBearer(sessionStore: SecureSessionStore) {
    bearer {
        cacheTokens = false
        sendWithoutRequest { true }
        loadTokens { sessionStore.current()?.let { BearerTokens(it.accessToken, it.refreshToken) } }
        refreshTokens {
            val refreshToken =
                oldTokens?.refreshToken
                    ?: sessionStore.current()?.refreshToken
                    ?: return@refreshTokens null
            val response =
                client.post(AuthResource.Refresh()) {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(refreshToken))
                }
            when {
                response.status.isSuccess() -> {
                    val minted = response.body<AccessTokenResponse>()
                    val session = ClientSession(minted.accessToken, refreshToken)
                    sessionStore.save(session)
                    BearerTokens(session.accessToken, session.refreshToken)
                }
                response.status == HttpStatusCode.Unauthorized -> {
                    sessionStore.clear()
                    null
                }
                else -> null
            }
        }
    }
}
