package org.example.project.feature.session

import androidx.datastore.core.DataStore
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.retryWhen
import org.example.project.core.local.storage.DataStoreFactory

/** Read-only callers see this same instance through [SessionStatus]. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SessionRepository>())
@ContributesBinding(AppScope::class, binding = binding<SessionStatus>())
class DefaultSessionRepository internal constructor(private val store: DataStore<StoredSession>) :
    SessionRepository {

    @Inject
    constructor(
        factory: DataStoreFactory,
    ) : this(
        factory.create(
            fileName = SESSION_FILE_NAME,
            serializer = StoredSession.serializer(),
            defaultValue = StoredSession.Absent,
            unreadableValue = StoredSession.Unreadable,
        ),
    )

    private val log = Logger.withTag("SessionRepository")

    // Collectors such as the root navigation live as long as the app, so a failed read must not
    // end the stream: it stays silent and picks up again once the file reads.
    override fun observe(): Flow<SessionState> =
        store.data
            .mapNotNull { stored ->
                stored.toStateOrNull().also { state ->
                    if (state == null) log.w { "session file unreadable; publishing no state" }
                }
            }
            .retryWhen { cause, _ ->
                log.w(cause) { "session read failed; publishing no state" }
                delay(READ_RETRY_DELAY)
                true
            }
            .distinctUntilChanged()

    override suspend fun snapshot(): Either<SessionRepositoryError, SessionState> =
        Either.catch { store.data.first() }
            .mapLeft { SessionRepositoryError.Storage(it) }
            .flatMap { stored ->
                stored.toStateOrNull()?.right()
                    ?: SessionRepositoryError.Storage(UnreadableSession()).left()
            }

    override suspend fun persist(session: ClientSession): Either<SessionRepositoryError, Unit> =
        write(StoredSession.Present(session))

    override suspend fun clear(): Either<SessionRepositoryError, Unit> = write(StoredSession.Absent)

    private suspend fun write(stored: StoredSession): Either<SessionRepositoryError, Unit> =
        Either.catch { store.updateData { stored } }
            .mapLeft { SessionRepositoryError.Storage(it) }
            .map {}

    private fun StoredSession.toStateOrNull(): SessionState? =
        when (this) {
            StoredSession.Absent -> SessionState.Unbound
            is StoredSession.Present -> SessionState.Bound(session)
            StoredSession.Unreadable -> null
        }

    private class UnreadableSession : IllegalStateException("session file present but unreadable")

    private companion object {
        const val SESSION_FILE_NAME = "session.json"
        val READ_RETRY_DELAY = 5.seconds
    }
}
