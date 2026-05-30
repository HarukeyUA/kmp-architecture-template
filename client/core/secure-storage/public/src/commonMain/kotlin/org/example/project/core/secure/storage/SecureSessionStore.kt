package org.example.project.core.secure.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** The signed-in session held on this device: the opaque bearer [token]. */
@Serializable data class ClientSession(val token: String)

/**
 * Platform-secure storage for the [ClientSession] — backed by the OS keystore on each platform
 * (Keychain / Android Keystore / encrypted file via KSafe), **never** plain DataStore: a Session is
 * a credential, not a setting (ADR-0009). The single gateway to read, observe, and mutate it.
 */
interface SecureSessionStore {
    /**
     * Emits the current session (or null when signed out) and re-emits on every [save] / [clear].
     */
    fun sessionFlow(): Flow<ClientSession?>

    suspend fun current(): ClientSession?

    suspend fun save(session: ClientSession)

    suspend fun clear()
}
