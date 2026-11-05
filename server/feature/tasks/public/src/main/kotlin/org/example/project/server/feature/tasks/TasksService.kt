package org.example.project.server.feature.tasks

import arrow.core.Either
import org.example.project.server.auth.Principal
import org.example.project.server.web.Failure
import org.example.project.shared.tasks.TasksCreateError
import org.example.project.shared.tasks.TasksDeleteError
import org.example.project.shared.tasks.TasksGetError
import org.example.project.shared.tasks.TasksSetDoneError

/** The caller's own tasks. A missing id, a malformed id and another account's id all look alike. */
interface TasksService {
    suspend fun list(principal: Principal): Either<Failure<Nothing>, List<Task>>

    suspend fun get(principal: Principal, taskId: String): Either<Failure<TasksGetError>, Task>

    suspend fun create(principal: Principal, title: String): Either<Failure<TasksCreateError>, Task>

    suspend fun setDone(
        principal: Principal,
        taskId: String,
        done: Boolean,
    ): Either<Failure<TasksSetDoneError>, Task>

    suspend fun delete(
        principal: Principal,
        taskId: String,
    ): Either<Failure<TasksDeleteError>, Unit>
}
