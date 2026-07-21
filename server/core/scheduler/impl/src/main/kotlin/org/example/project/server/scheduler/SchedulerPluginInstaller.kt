package org.example.project.server.scheduler

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.server.application.Application
import org.example.project.server.web.PluginInstaller
import org.example.project.server.web.PluginOrder

/**
 * Starts the [AdvisoryLockScheduler]'s job loops when the app boots. It self-registers as a
 * [PluginInstaller] (like every other startup hook), so wiring the scheduler — and any job a domain
 * contributes — touches **zero lines** in `:server:app`. It installs no Ktor plugin; it just hands
 * the application's [CoroutineScope][kotlinx.coroutines.CoroutineScope] to the scheduler, so the
 * loops live and die with the application.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class SchedulerPluginInstaller(private val scheduler: AdvisoryLockScheduler) : PluginInstaller {
    override val order: PluginOrder = PluginOrder.SCHEDULER

    override fun Application.install() {
        scheduler.start(this)
    }
}
