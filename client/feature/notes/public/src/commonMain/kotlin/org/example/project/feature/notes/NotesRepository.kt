package org.example.project.feature.notes

import arrow.core.Either
import org.example.project.core.error.AppError

/**
 * Client-side notes against the server's `:shared:notes` contract. The session bearer token is
 * attached automatically by the shared `HttpClient` (Auth plugin), so these calls just speak the
 * typed `@Resource`. Wire DTOs are mapped to the client [Note] model via `toModel()` — the client
 * shares the *wire*, not the server's domain type (ADR-0006). Failures arrive through the existing
 * typed [AppError] pipeline (a 4xx `ErrorEnvelope` becomes `NetworkError.Api(ApiError)`).
 */
interface NotesRepository {
    suspend fun list(): Either<AppError, List<Note>>

    suspend fun create(text: String): Either<AppError, Unit>

    suspend fun delete(id: String): Either<AppError, Unit>
}
