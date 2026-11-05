package org.example.project.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "tasks", indices = [Index("created_at")])
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "done") val done: Boolean,
    /** Epoch microseconds, the server's precision, so the cached order matches the server's. */
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
