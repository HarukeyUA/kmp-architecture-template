package org.example.project.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import org.example.project.core.database.AppDatabase
import org.example.project.core.database.AppDatabaseConstructor
import org.example.project.core.database.DATABASE_FILE_NAME

@BindingContainer
@ContributesTo(AppScope::class)
object AndroidDatabaseGraph {
    @Provides
    fun provideAppDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> =
        Room.databaseBuilder(context, DATABASE_FILE_NAME, AppDatabaseConstructor::initialize)
}
