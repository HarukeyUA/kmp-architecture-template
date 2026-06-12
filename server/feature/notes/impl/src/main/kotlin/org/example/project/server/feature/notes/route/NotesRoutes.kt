package org.example.project.server.feature.notes.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.feature.notes.AuthoredNote
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.serve
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NotesApi

/**
 * Self-registering notes routes (ADR-0006) — added with zero lines in `:server:app`. Every route
 * lives under [authenticatedRoutes], so the session middleware 401s before the handler runs and
 * [principal] is always present. The route is dumb and owns the Wire boundary (ADR-0003): each
 * [serve] binds a typed [NotesApi] endpoint, unpacks the request, calls the service, maps the
 * returned domain model to the response DTO, and folds `Either<ApiError, T>` to HTTP.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class NotesRoutes(private val service: NotesService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            authenticatedRoutes {
                serve(NotesApi.list, HttpStatusCode.OK) { _ ->
                    service.list(principal()).map { notes ->
                        NoteListResponse(notes.map { it.toResponse() })
                    }
                }
                serve(NotesApi.create, HttpStatusCode.Created) { _, body ->
                    service.create(principal(), body.text).map { it.toResponse() }
                }
                serve(NotesApi.delete, HttpStatusCode.NoContent) { resource ->
                    service.delete(principal(), resource.id)
                }
            }
        }
    }
}

private fun AuthoredNote.toResponse(): NoteResponse =
    NoteResponse(
        id = note.id,
        text = note.text,
        authorEmail = authorEmail,
        createdAt = note.createdAt,
    )
