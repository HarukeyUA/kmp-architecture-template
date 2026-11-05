package org.example.project.feature.session

import arrow.core.Either
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.example.project.core.database.DatabaseWiper

@Inject
@ContributesBinding(AppScope::class)
class DefaultSessionEndWiper(
    private val sessions: SessionStatus,
    private val wiper: DatabaseWiper,
) : SessionEndWiper {
    private val log = Logger.withTag("SessionEndWiper")

    override suspend fun watch() {
        var previous: String? = null
        sessions
            .observe()
            .map { (it as? SessionState.Bound)?.session?.refreshToken }
            .distinctUntilChanged()
            .collect { current ->
                // One sign-in replacing another also ends the first: a logout that could not clear
                // the session reaches the next login without ever passing through signed out.
                if (current == null || (previous != null && current != previous)) wipeUntilDone()
                previous = current
            }
    }

    // Giving up would leave this account's rows for the next one, so a failure can only be retried.
    private suspend fun wipeUntilDone() {
        while (Either.catch { wiper.wipe() }.onLeft { log.e(it) { "wipe failed" } }.isLeft()) {
            delay(RETRY_DELAY)
        }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
