package org.example.project.core.database.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.cinterop.ExperimentalForeignApi
import org.example.project.core.database.AppDatabase
import org.example.project.core.database.AppDatabaseConstructor
import org.example.project.core.database.DATABASE_FILE_NAME
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@BindingContainer
@ContributesTo(AppScope::class)
object AppleDatabaseGraph {
    @OptIn(ExperimentalForeignApi::class)
    @Provides
    fun provideAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val documentDirectory: NSURL? =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        return Room.databaseBuilder(
            name = "${requireNotNull(documentDirectory?.path)}/$DATABASE_FILE_NAME",
            factory = AppDatabaseConstructor::initialize,
        )
    }
}
