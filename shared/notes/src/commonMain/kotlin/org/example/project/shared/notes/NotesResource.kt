package org.example.project.shared.notes

import io.ktor.http.HttpMethod
import io.ktor.resources.Resource
import org.example.project.shared.common.Endpoint

/**
 * The typed notes routes under `/v1`, the single source of truth shared by `ktor-client-resources`
 * and `ktor-server-resources`. Every route sits behind the session middleware (a valid Session is
 * required), so the server resolves the owning account from the Principal, never from the path.
 *
 * `GET /v1/notes` lists the caller's notes; `POST /v1/notes` creates one; `DELETE /v1/notes/{id}`
 * removes one the caller owns.
 */
@Resource("/v1/notes")
class NotesResource {
    @Resource("{id}") class ById(val parent: NotesResource = NotesResource(), val id: String)
}

/**
 * The notes domain's operation contracts, co-located with the [NotesResource] routes they bind.
 * [list] and [create] target the same `/v1/notes` collection but differ by method, body, and
 * response — the operation, not the resource, is the unit that owns a body and a return type.
 * Client (`HttpClient.call`) and server (`Route.serve`) both consume these.
 */
object NotesApi {
    val list: Endpoint<NotesResource, Unit, NoteListResponse> =
        Endpoint(HttpMethod.Get, request = null, response = NoteListResponse.serializer())

    val create: Endpoint<NotesResource, CreateNoteRequest, NoteResponse> =
        Endpoint(HttpMethod.Post, CreateNoteRequest.serializer(), NoteResponse.serializer())

    val delete: Endpoint<NotesResource.ById, Unit, Unit> =
        Endpoint(HttpMethod.Delete, request = null, response = null)
}
