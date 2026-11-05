package org.example.project.server.feature.tasks.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.Principal
import org.example.project.server.feature.tasks.Task
import org.example.project.server.feature.tasks.TasksService
import org.example.project.server.feature.tasks.data.TaskRepository
import org.example.project.server.web.Failure
import org.example.project.server.web.declared
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskTitle
import org.example.project.shared.tasks.TaskValidationFailed
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksSetDoneError

@Inject
@ContributesBinding(AppScope::class)
class DefaultTasksService(private val repo: TaskRepository) : TasksService {
    override suspend fun list(principal: Principal): Either<Failure<Nothing>, List<Task>> = either {
        repo.listFor(principal.accountId)
    }

    override suspend fun get(
        principal: Principal,
        taskId: String,
    ): Either<Failure<TasksGetError>, Task> = either {
        repo.find(principal.accountId, taskId) ?: declared(TaskNotFound)
    }

    override suspend fun create(
        principal: Principal,
        title: String,
    ): Either<Failure<TasksCreateError>, Task> = either {
        val taskTitle = TaskTitle.of(title).getOrElse { declared(TaskValidationFailed(listOf(it))) }
        repo.create(principal.accountId, taskTitle.value)
    }

    override suspend fun setDone(
        principal: Principal,
        taskId: String,
        done: Boolean,
    ): Either<Failure<TasksSetDoneError>, Task> = either {
        repo.setDone(principal.accountId, taskId, done) ?: declared(TaskNotFound)
    }

    override suspend fun delete(
        principal: Principal,
        taskId: String,
    ): Either<Failure<TasksDeleteError>, Unit> = either {
        if (!repo.delete(principal.accountId, taskId)) declared(TaskNotFound)
    }
}
