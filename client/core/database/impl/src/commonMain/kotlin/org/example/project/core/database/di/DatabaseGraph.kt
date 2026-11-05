package org.example.project.core.database.di

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import org.example.project.core.database.AppDatabase
import org.example.project.core.database.dao.TaskDao
import org.example.project.core.dispatchers.IoDispatcher

@BindingContainer
@ContributesTo(AppScope::class)
object DatabaseGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppDatabase(
        builder: RoomDatabase.Builder<AppDatabase>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AppDatabase =
        builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(ioDispatcher)
            // Everything here is a cache the server can refill, so a schema bump needs no
            // migration.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
}
