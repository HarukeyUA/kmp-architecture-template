package org.example.project.core.secure.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * The signed-in state held on this device (ADR-0009 as amended): the short-lived JWT [accessToken]
 * sent as the bearer on every request, and the opaque [refreshToken] presented only to the refresh
 * and logout endpoints to mint replacements / revoke the session.
 */
@Serializable data class ClientSession(val accessToken: String, val refreshToken: String)

/**
 * Platform-secure storage for the [ClientSession] — backed by the OS keystore on each platform
 * (Keychain / Android Keystore / encrypted file via KSafe), **never** plain DataStore: both tokens
 * are credentials, not settings (ADR-0009). The single gateway to read, observe, and mutate it.
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
