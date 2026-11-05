package org.example.project.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entity.TaskEntity

@Dao
interface TaskDao {
    /** Newest first, in the order the server lists them. */
    @Query("SELECT * FROM tasks ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id") fun observeById(id: String): Flow<TaskEntity?>

    @Upsert suspend fun upsert(task: TaskEntity)

    @Upsert suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks") suspend fun deleteAll()
}
