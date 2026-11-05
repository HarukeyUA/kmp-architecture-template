package org.example.project.shared.tasks

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.FieldError

@Serializable sealed interface TasksGetError : ApiError

@Serializable sealed interface TasksCreateError : ApiError

@Serializable sealed interface TasksSetDoneError : ApiError

@Serializable sealed interface TasksDeleteError : ApiError

/** Also what another account's task answers with, so a task's existence is never disclosed. */
@Serializable
@SerialName("tasks.not_found")
data object TaskNotFound : TasksGetError, TasksSetDoneError, TasksDeleteError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.NotFound
}

@Serializable
@SerialName("tasks.validation")
data class TaskValidationFailed(@SerialName("fields") val fields: List<FieldError>) :
    TasksCreateError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.UnprocessableEntity
}
