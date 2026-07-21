package org.example.project.server.scheduler

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(AppScope::class)
interface SchedulerProviders {
    /**
     * The contributed [ScheduledJob]s. `allowEmpty = true` so a server with no jobs still assembles
     * — the set is empty until a core or domain `:impl` ships one via `@ContributesIntoSet`.
     */
    @Multibinds(allowEmpty = true) fun scheduledJobs(): Set<ScheduledJob>
}
