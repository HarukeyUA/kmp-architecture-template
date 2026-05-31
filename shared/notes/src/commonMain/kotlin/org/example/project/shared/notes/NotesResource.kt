package org.example.project.shared.notes

import io.ktor.resources.Resource

/**
 * The typed notes routes under `/v1`, the single source of truth shared by `ktor-client-resources`
 * and `ktor-server-resources` (ADR-0002) — the same shape Phase 4 proved for auth, replicated for a
 * second domain. Every route sits behind the session middleware (a valid Session is required), so
 * the server resolves the owning account from the Principal, never from the path.
 *
 * `GET /v1/notes` lists the caller's notes; `POST /v1/notes` creates one; `DELETE /v1/notes/{id}`
 * removes one the caller owns.
 */
@Resource("/v1/notes")
class NotesResource {
    @Resource("{id}") class ById(val parent: NotesResource = NotesResource(), val id: String)
}
