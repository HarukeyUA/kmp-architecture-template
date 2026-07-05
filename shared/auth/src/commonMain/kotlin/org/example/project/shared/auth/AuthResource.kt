package org.example.project.shared.auth

import io.ktor.http.HttpMethod
import io.ktor.resources.Resource
import org.example.project.shared.common.Endpoint

/**
 * The typed auth routes under `/v1`, the single source of truth for paths shared by
 * `ktor-client-resources` and `ktor-server-resources`. `@Resource` is `@MetaSerializable`, so these
 * are serializable without a separate `@Serializable`.
 *
 * `signup` / `login` / `refresh` are open (`refresh` authenticates via the refresh token in its
 * body); `logout` and `me` require a valid access token.
 */
@Resource("/v1/auth")
class AuthResource {
    @Resource("signup") class Signup(val parent: AuthResource = AuthResource())

    @Resource("login") class Login(val parent: AuthResource = AuthResource())

    @Resource("refresh") class Refresh(val parent: AuthResource = AuthResource())

    @Resource("logout") class Logout(val parent: AuthResource = AuthResource())

    @Resource("me") class Me(val parent: AuthResource = AuthResource())
}

/**
 * The auth domain's operation contracts, co-located with the [AuthResource] routes they bind: each
 * ties a route to the method, request body, and response that travel over it. Client
 * (`HttpClient.call`) and server (`Route.serve`) both consume these, so the wire operation is
 * defined once.
 */
object AuthApi {
    val signup: Endpoint<AuthResource.Signup, SignupRequest, TokensResponse> =
        Endpoint(HttpMethod.Post, SignupRequest.serializer(), TokensResponse.serializer())

    val login: Endpoint<AuthResource.Login, LoginRequest, TokensResponse> =
        Endpoint(HttpMethod.Post, LoginRequest.serializer(), TokensResponse.serializer())

    val refresh: Endpoint<AuthResource.Refresh, RefreshRequest, AccessTokenResponse> =
        Endpoint(HttpMethod.Post, RefreshRequest.serializer(), AccessTokenResponse.serializer())

    val logout: Endpoint<AuthResource.Logout, LogoutRequest, Unit> =
        Endpoint(HttpMethod.Post, LogoutRequest.serializer(), response = null)

    val me: Endpoint<AuthResource.Me, Unit, AccountResponse> =
        Endpoint(HttpMethod.Get, request = null, response = AccountResponse.serializer())
}
