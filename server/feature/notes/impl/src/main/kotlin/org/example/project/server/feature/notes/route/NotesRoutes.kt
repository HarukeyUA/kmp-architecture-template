package org.example.project.server.feature.notes.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.serve
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NotesApi

/**
 * Self-registering notes routes (ADR-0006) — added with zero lines in `:server:app`. Every route
 * lives under [authenticatedRoutes], so the session middleware 401s before the handler runs and
 * [principal] is always present. The route stays dumb: each [serve] binds a typed [NotesApi]
 * endpoint, resolves the Principal, calls the service, and folds its `Either<ApiError, T>` to HTTP.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class NotesRoutes(private val service: NotesService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            authenticatedRoutes {
                serve(NotesApi.list, HttpStatusCode.OK) { _, _ ->
                    service.list(principal()).map(::NoteListResponse)
                }
                serve(NotesApi.create, HttpStatusCode.Created) { _, body ->
                    service.create(principal(), body)
                }
                serve(NotesApi.delete, HttpStatusCode.NoContent) { resource, _ ->
                    service.delete(principal(), resource.id)
                }
            }
        }
    }
}
