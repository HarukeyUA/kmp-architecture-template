package org.example.project.server.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.example.project.server.auth.data.Sessions
import org.example.project.server.database.dbTransaction
import org.example.project.server.scheduler.ScheduledJob
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere

/**
 * The worked example of a [ScheduledJob] (ADR-0010): periodically deletes sessions past their
 * expiry. Expired sessions already fail to resolve (the store checks `expiresAt`), so this is pure
 * housekeeping — which is exactly what makes it **idempotent**: deleting the already-expired rows
 * again removes nothing, so it is safe to run on whichever node wins the advisory lock each tick.
 *
 * It self-registers via `@ContributesIntoSet`, so it joins the scheduler's `Set<ScheduledJob>`
 * without `:server:app` or the scheduler core knowing auth exists.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class ExpiredSessionSweeper : ScheduledJob {
    override val name: String = "auth.expired-session-sweep"
    override val interval: Duration = 1.hours

    override suspend fun run() {
        dbTransaction { Sessions.deleteWhere { Sessions.expiresAt less Clock.System.now() } }
    }
}
