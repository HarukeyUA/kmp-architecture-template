package org.example.project.core.database

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class DefaultDatabaseWiper(private val database: AppDatabase) : DatabaseWiper {
    override suspend fun wipe() {
        database.clearAllTables()
    }
}
