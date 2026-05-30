package org.example.project.shared.auth

import io.ktor.resources.Resource

/**
 * The typed auth routes under `/v1`, the single source of truth for paths shared by
 * `ktor-client-resources` and `ktor-server-resources` (ADR-0002). `@Resource` is
 * `@MetaSerializable`, so these are serializable without a separate `@Serializable`.
 *
 * `signup` / `login` are open; `logout` and `me` require a valid Session.
 */
@Resource("/v1/auth")
class AuthResource {
    @Resource("signup") class Signup(val parent: AuthResource = AuthResource())

    @Resource("login") class Login(val parent: AuthResource = AuthResource())

    @Resource("logout") class Logout(val parent: AuthResource = AuthResource())

    @Resource("me") class Me(val parent: AuthResource = AuthResource())
}
