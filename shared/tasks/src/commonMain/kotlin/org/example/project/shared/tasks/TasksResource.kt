package org.example.project.shared.tasks

import io.ktor.http.HttpMethod
import io.ktor.resources.Resource
import kotlinx.serialization.serializer
import org.example.project.shared.common.Endpoint

/** Every route requires an access token; the owning account comes from it, never from the path. */
@Resource("/v1/tasks")
class TasksResource {
    @Resource("{id}") class ById(val parent: TasksResource = TasksResource(), val id: String)
}

object TasksApi {
    /** The caller's tasks, newest first. */
    val list: Endpoint<TasksResource, Unit, TaskListResponse, Nothing> =
        Endpoint(
            HttpMethod.Get,
            request = null,
            response = TaskListResponse.serializer(),
            error = null,
        )

    val get: Endpoint<TasksResource.ById, Unit, TaskResponse, TasksGetError> =
        Endpoint(
            HttpMethod.Get,
            request = null,
            response = TaskResponse.serializer(),
            error = serializer<TasksGetError>(),
        )

    val create: Endpoint<TasksResource, CreateTaskRequest, TaskResponse, TasksCreateError> =
        Endpoint(
            HttpMethod.Post,
            CreateTaskRequest.serializer(),
            TaskResponse.serializer(),
            error = serializer<TasksCreateError>(),
        )

    val setDone: Endpoint<TasksResource.ById, SetTaskDoneRequest, TaskResponse, TasksSetDoneError> =
        Endpoint(
            HttpMethod.Patch,
            SetTaskDoneRequest.serializer(),
            TaskResponse.serializer(),
            error = serializer<TasksSetDoneError>(),
        )

    val delete: Endpoint<TasksResource.ById, Unit, Unit, TasksDeleteError> =
        Endpoint(
            HttpMethod.Delete,
            request = null,
            response = null,
            error = serializer<TasksDeleteError>(),
        )
}
