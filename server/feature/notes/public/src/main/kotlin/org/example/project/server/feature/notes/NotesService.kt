package org.example.project.server.feature.notes

import arrow.core.Either
import org.example.project.server.auth.Principal
import org.example.project.shared.common.ApiError
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteResponse

/**
 * The notes domain service: per-account CRUD over the caller's own notes. Every method takes the
 * authenticated [Principal] (the route supplies it from the session middleware) and returns
 * `Either<ApiError, T>`; the route folds that to HTTP via `respondEither`. The notes domain holds
 * only an opaque `account_id` and reaches the auth domain through its **public** service for any
 * account detail (ADR-0006).
 */
interface NotesService {
    suspend fun list(principal: Principal): Either<ApiError, List<NoteResponse>>

    suspend fun create(
        principal: Principal,
        request: CreateNoteRequest,
    ): Either<ApiError, NoteResponse>

    suspend fun delete(principal: Principal, noteId: String): Either<ApiError, Unit>

    companion object {
        /**
         * The per-account character budget across all of an account's notes. Exceeding it yields a
         * typed [org.example.project.shared.notes.NotesQuotaExceeded]. Lives on the contract so
         * callers (and tests) share one source of truth.
         */
        const val QUOTA = 20_000
    }
}
