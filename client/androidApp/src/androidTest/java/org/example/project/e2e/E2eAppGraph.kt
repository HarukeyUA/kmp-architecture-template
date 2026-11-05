package org.example.project.e2e

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import org.example.project.AndroidAppGraph
import org.example.project.androidApiConfigDefaults
import org.example.project.core.buildinfo.Environment
import org.example.project.core.database.dao.TaskDao
import org.example.project.core.network.ApiConfigDefaults

/**
 * The graph the E2E suite runs against: the production Android graph plus accessors the flows need
 * to act below a boundary the UI cannot cross. Lives in `androidTest` so a shipped build cannot
 * name them. Metro re-aggregates the same `AppScope` contributions, so nothing here is a
 * substitution.
 */
@DependencyGraph(AppScope::class)
interface E2eAppGraph : AndroidAppGraph {
    val taskDao: TaskDao

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides applicationContext: Context,
            @Provides environment: Environment,
            @Provides apiConfigDefaults: ApiConfigDefaults,
        ): E2eAppGraph
    }
}

/** Mirrors `createAndroidAppGraph`, down to how the dev server's base URL is resolved. */
fun createE2eAppGraph(
    applicationContext: Context,
    environment: Environment,
    devServerHost: String?,
): E2eAppGraph =
    createGraphFactory<E2eAppGraph.Factory>()
        .create(applicationContext, environment, androidApiConfigDefaults(devServerHost))
