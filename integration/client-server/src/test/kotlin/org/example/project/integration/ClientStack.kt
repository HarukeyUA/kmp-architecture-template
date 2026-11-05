package org.example.project.integration

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import org.example.project.feature.auth.AuthRepository
import org.example.project.feature.auth.data.AuthRepositoryImpl
import org.example.project.feature.session.FakeSessionRepository
import org.example.project.feature.session.sessionBearer
import org.example.project.server.testing.seamJson

/**
 * The client side of a client↔server integration test: the production repositories wired exactly
 * like the app graph wires them — seam `Json`, typed `@Resource`s, [sessionBearer] with its
 * refresh/clear coordination over an in-memory [FakeSessionRepository] — except the `HttpClient` is
 * the `testApplication` transport, so every call lands in the in-process `:server:*` stack booted
 * by `serverTest`.
 */
class ClientStack(val http: HttpClient, val sessions: FakeSessionRepository) {
    val auth: AuthRepository = AuthRepositoryImpl(http, sessions)
}

/** Builds a fresh [ClientStack] — one signed-out "device" — inside a `serverTest` block. */
fun ApplicationTestBuilder.clientStack(): ClientStack {
    val sessions = FakeSessionRepository()
    val http = createClient {
        install(ContentNegotiation) { json(seamJson) }
        install(Resources)
        install(Auth) { sessionBearer(sessions) }
    }
    return ClientStack(http, sessions)
}
