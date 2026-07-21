package org.example.project.core.secure.storage

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.Flow

/**
 * Stores the [ClientSession] as a single `@Serializable` blob in KSafe, which encrypts it with the
 * platform's hardware-backed keystore. The platform [KSafe] instance is provided per target.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class KSafeSecureSessionStore(private val ksafe: KSafe) : SecureSessionStore {
    override fun sessionFlow(): Flow<ClientSession?> = ksafe.getFlow<ClientSession?>(KEY, null)

    override suspend fun current(): ClientSession? = ksafe.get<ClientSession?>(KEY, null)

    override suspend fun save(session: ClientSession) {
        ksafe.put<ClientSession?>(KEY, session)
    }

    override suspend fun clear() {
        ksafe.delete(KEY)
    }

    private companion object {
        const val KEY = "client_session"
    }
}
