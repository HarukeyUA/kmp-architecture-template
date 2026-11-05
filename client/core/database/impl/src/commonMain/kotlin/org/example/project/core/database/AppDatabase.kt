package org.example.project.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import org.example.project.core.database.dao.TaskDao
import org.example.project.core.database.entity.TaskEntity

@Database(entities = [TaskEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}

// Room's KSP processor generates the actual for each target.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val DATABASE_FILE_NAME = "app.db"
