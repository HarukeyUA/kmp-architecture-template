package org.example.project.core.database.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import java.io.File
import org.example.project.core.database.AppDatabase
import org.example.project.core.database.AppDatabaseConstructor
import org.example.project.core.database.DATABASE_FILE_NAME

@BindingContainer
@ContributesTo(AppScope::class)
object JvmDatabaseGraph {
    @Provides
    fun provideAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
        Room.databaseBuilder(
            name = File(FileKit.filesDir.path, DATABASE_FILE_NAME).absolutePath,
            factory = AppDatabaseConstructor::initialize,
        )
}
