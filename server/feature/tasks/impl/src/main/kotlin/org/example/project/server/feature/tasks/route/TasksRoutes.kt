package org.example.project.server.feature.tasks.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.feature.tasks.Task
import org.example.project.server.feature.tasks.TasksService
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.serve
import org.example.project.shared.tasks.TaskListResponse
import org.example.project.shared.tasks.TaskResponse
import org.example.project.shared.tasks.TasksApi

@Inject
@ContributesIntoSet(AppScope::class)
class TasksRoutes(private val service: TasksService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            authenticatedRoutes {
                serve(TasksApi.list, HttpStatusCode.OK) { _ ->
                    service.list(principal()).map { tasks ->
                        TaskListResponse(tasks.map { it.toResponse() })
                    }
                }
                serve(TasksApi.get, HttpStatusCode.OK) { resource ->
                    service.get(principal(), resource.id).map { it.toResponse() }
                }
                serve(TasksApi.create, HttpStatusCode.Created) { _, body ->
                    service.create(principal(), body.title).map { it.toResponse() }
                }
                serve(TasksApi.setDone, HttpStatusCode.OK) { resource, body ->
                    service.setDone(principal(), resource.id, body.done).map { it.toResponse() }
                }
                serve(TasksApi.delete, HttpStatusCode.NoContent) { resource ->
                    service.delete(principal(), resource.id)
                }
            }
        }
    }
}

private fun Task.toResponse(): TaskResponse =
    TaskResponse(id = id, title = title, done = done, createdAt = createdAt)
