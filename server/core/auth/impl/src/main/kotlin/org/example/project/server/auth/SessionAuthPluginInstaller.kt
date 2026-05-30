package org.example.project.server.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/**
 * Installs the `session` bearer-auth provider that backs [authenticatedRoutes]. The bearer token is
 * resolved to a [Principal] via the [SessionStore] (cache → DB); an unresolvable token yields 401.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class SessionAuthPluginInstaller(private val sessionStore: SessionStore) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.AUTHENTICATION

    override fun Application.install() {
        install(Authentication) {
            bearer(SESSION_AUTH) {
                authenticate { credential -> sessionStore.resolve(credential.token) }
            }
        }
    }
}
