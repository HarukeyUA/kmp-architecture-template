package org.example.project.integration

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.core.network.sessionBearer
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore
import org.example.project.feature.auth.AuthRepository
import org.example.project.feature.auth.data.AuthRepositoryImpl
import org.example.project.feature.notes.NotesRepository
import org.example.project.feature.notes.data.NotesRepositoryImpl
import org.example.project.server.testing.seamJson

/**
 * The client side of a client↔server integration test: the production repositories wired exactly
 * like the app graph wires them — seam `Json`, typed `@Resource`s, [sessionBearer] with its
 * refresh/clear coordination over an in-memory [SecureSessionStore] — except the `HttpClient` is
 * the `testApplication` transport, so every call lands in the in-process `:server:*` stack booted
 * by `serverTest`.
 */
class ClientStack(val http: HttpClient, val sessionStore: SecureSessionStore) {
    val auth: AuthRepository = AuthRepositoryImpl(http, sessionStore)
    val notes: NotesRepository = NotesRepositoryImpl(http)
}

/** Builds a fresh [ClientStack] — one signed-out "device" — inside a `serverTest` block. */
fun ApplicationTestBuilder.clientStack(): ClientStack {
    val sessionStore = InMemorySessionStore()
    val http = createClient {
        install(ContentNegotiation) { json(seamJson) }
        install(Resources)
        install(Auth) { sessionBearer(sessionStore) }
    }
    return ClientStack(http, sessionStore)
}

/** The secure store's in-memory stand-in; the platform keystore is out of scope on the JVM. */
private class InMemorySessionStore : SecureSessionStore {
    private val state = MutableStateFlow<ClientSession?>(null)

    override fun sessionFlow(): Flow<ClientSession?> = state

    override suspend fun current(): ClientSession? = state.value

    override suspend fun save(session: ClientSession) {
        state.value = session
    }

    override suspend fun clear() {
        state.value = null
    }
}
