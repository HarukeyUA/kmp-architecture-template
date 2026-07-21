package org.example.project.server.feature.notes

import arrow.core.Either
import org.example.project.server.auth.Principal
import org.example.project.server.web.Failure
import org.example.project.shared.notes.NotesCreateError

/**
 * The notes domain service: per-account CRUD over the caller's own notes. Every method takes the
 * authenticated [Principal] (the route supplies it from the session middleware) and returns a
 * `Either<Failure<Err>, T>` over **domain types** (ADR-0003 as amended; ADR-0011) — the route owns
 * the Wire mapping. Only [create] declares an error ([NotesCreateError]); [list] and [delete]
 * declare none, so their failure channel is purely Ambient (`Failure<Nothing>`). The notes domain
 * holds only an opaque `account_id` and reaches the auth domain through its **public** service for
 * any account detail (ADR-0006).
 */
interface NotesService {
    suspend fun list(principal: Principal): Either<Failure<Nothing>, List<AuthoredNote>>

    suspend fun create(
        principal: Principal,
        text: String,
    ): Either<Failure<NotesCreateError>, AuthoredNote>

    suspend fun delete(principal: Principal, noteId: String): Either<Failure<Nothing>, Unit>

    companion object {
        /**
         * The per-account budget across all of an account's notes, in **Unicode code points** (the
         * unit shared by Kotlin's surrogate-aware count and Postgres `char_length` — see
         * [org.example.project.shared.notes.NoteText]). Exceeding it yields a typed
         * [org.example.project.shared.notes.NotesQuotaExceeded]. Lives on the contract so callers
         * (and tests) share one source of truth. Sized demo-small so the template's quota path is
         * actually triggerable from the UI.
         */
        const val QUOTA = 2_000
    }
}
