package org.example.project.server.feature.tasks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import org.example.project.server.database.TableSet
import org.example.project.server.feature.tasks.data.Tasks

@ContributesTo(AppScope::class)
interface TasksFeatureBindings {
    @Provides @IntoSet fun tasksTableSet(): TableSet = TableSet(Tasks)
}
