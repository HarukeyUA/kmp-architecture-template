package org.example.project.core.database.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.example.project.core.database.dao.TaskDao
import org.example.project.core.database.entity.TaskEntity

class FakeTaskDao(initial: List<TaskEntity> = emptyList()) : TaskDao {
    private val rows = MutableStateFlow(initial.associateBy { it.id })

    val current: List<TaskEntity>
        get() = rows.value.values.sortedWith(NEWEST_FIRST)

    override fun observeAll(): Flow<List<TaskEntity>> = rows.map {
        it.values.sortedWith(NEWEST_FIRST)
    }

    override fun observeById(id: String): Flow<TaskEntity?> = rows.map { it[id] }

    override suspend fun upsert(task: TaskEntity) {
        rows.update { it + (task.id to task) }
    }

    override suspend fun upsertAll(tasks: List<TaskEntity>) {
        rows.update { current -> current + tasks.associateBy { it.id } }
    }

    override suspend fun deleteById(id: String) {
        rows.update { it - id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }

    private companion object {
        val NEWEST_FIRST =
            compareByDescending<TaskEntity> { it.createdAt }.thenByDescending { it.id }
    }
}
