package org.example.project.server.feature.notes.route

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.example.project.server.auth.authenticatedRoutes
import org.example.project.server.auth.principal
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.web.RouteRegistrar
import org.example.project.server.web.respondEither
import org.example.project.shared.notes.NoteListResponse
import org.example.project.shared.notes.NotesResource

/**
 * Self-registering notes routes (ADR-0006) — added with zero lines in `:server:app`. Every route
 * lives under [authenticatedRoutes], so the session middleware 401s before the handler runs and
 * [principal] is always present. The route stays dumb: resolve the Principal, call the service,
 * fold its `Either<ApiError, T>` to HTTP via [respondEither].
 */
@Inject
@ContributesIntoSet(AppScope::class)
class NotesRoutes(private val service: NotesService) : RouteRegistrar {
    override fun Application.register() {
        routing {
            authenticatedRoutes {
                get<NotesResource> {
                    call.respondEither(service.list(principal())) {
                        respond(HttpStatusCode.OK, NoteListResponse(it))
                    }
                }
                post<NotesResource> {
                    call.respondEither(service.create(principal(), call.receive())) {
                        respond(HttpStatusCode.Created, it)
                    }
                }
                delete<NotesResource.ById> { resource ->
                    call.respondEither(service.delete(principal(), resource.id)) {
                        respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
