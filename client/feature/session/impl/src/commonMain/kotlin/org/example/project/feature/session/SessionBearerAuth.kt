package org.example.project.feature.session

import arrow.core.raise.catch
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.example.project.core.network.HttpClientConfigurer
import org.example.project.shared.auth.AccessTokenResponse
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.RefreshRequest
import org.example.project.shared.auth.SessionExpired
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.decodeDeclaredApiError

private val log = Logger.withTag("SessionBearer")

/** Puts [sessionBearer] on the app's HttpClient. */
@Inject
@ContributesIntoSet(AppScope::class)
class SessionBearerConfigurer(private val sessions: SessionRepository) : HttpClientConfigurer {
    override fun configure(config: HttpClientConfig<*>) {
        config.install(Auth) { sessionBearer(sessions) }
    }
}

/**
 * Attaches the stored access token and refreshes it through `POST /v1/auth/refresh` on a 401. Only
 * [SessionExpired] from that endpoint signs the device out, so a 5xx, a dropped connection or a
 * failed session read cannot.
 */
fun AuthConfig.sessionBearer(sessions: SessionRepository) {
    bearer {
        cacheTokens = false
        sendWithoutRequest { true }
        loadTokens {
            sessions.boundSessionOrNull(log)?.let { BearerTokens(it.accessToken, it.refreshToken) }
        }
        refreshTokens {
            val refreshToken =
                oldTokens?.refreshToken
                    ?: sessions.boundSessionOrNull(log)?.refreshToken
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
                    // The retry can still use the minted token when it could not be stored.
                    sessions.persist(session).onLeft { log.w { "minted token not stored: $it" } }
                    BearerTokens(session.accessToken, session.refreshToken)
                }
                else -> {
                    if (response.isSessionExpired()) {
                        sessions.clear().onLeft { log.w { "expired session not cleared: $it" } }
                    }
                    null
                }
            }
        }
    }
}

/** Parsing is best-effort: a non-envelope body (5xx page, truncated response) is not an expiry. */
private suspend fun HttpResponse.isSessionExpired(): Boolean =
    catch(
        {
            AuthApi.refresh.error?.let { lens ->
                decodeDeclaredApiError(lens, body<ErrorEnvelope>().error)
            } is SessionExpired
        },
    ) {
        false
    }
