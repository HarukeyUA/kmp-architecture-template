package org.example.project.feature.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.core.secure.storage.ClientSession
import org.example.project.core.secure.storage.SecureSessionStore

/** In-memory [SecureSessionStore] for tests (no platform keystore). */
class InMemorySecureSessionStore : SecureSessionStore {
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
