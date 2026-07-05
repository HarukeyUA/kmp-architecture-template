package org.example.project.feature.notes

import arrow.core.Either
import org.example.project.core.error.CallFailure
import org.example.project.shared.notes.NotesCreateError

/**
 * Client-side notes against the server's `:shared:notes` contract. The session bearer token is
 * attached automatically by the shared `HttpClient` (Auth plugin), so these calls just speak the
 * typed `@Resource`. Wire DTOs are mapped to the client [Note] model via `toModel()` — the client
 * shares the *wire*, not the server's domain type (ADR-0006). Failures arrive as a typed
 * [CallFailure] carrying each operation's Declared errors (ADR-0011): only `create` declares one
 * ([NotesCreateError]); `list` and `delete` declare nothing ([Nothing]).
 */
interface NotesRepository {
    suspend fun list(): Either<CallFailure<Nothing>, List<Note>>

    suspend fun create(text: String): Either<CallFailure<NotesCreateError>, Unit>

    suspend fun delete(id: String): Either<CallFailure<Nothing>, Unit>
}
