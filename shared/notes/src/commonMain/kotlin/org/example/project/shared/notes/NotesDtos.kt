package org.example.project.shared.notes

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** A note to create. Its [text] is shape-validated on both sides via [NoteText]. */
@Serializable data class CreateNoteRequest(val text: String)

/**
 * One note as it crosses the wire. [authorEmail] is resolved server-side through the
 * **cross-domain** call into `AuthService` (ADR-0006) — the notes domain holds only an opaque
 * `account_id`, never the accounts table, so it asks the auth domain's public service for the
 * address. [createdAt] is the honest UTC [Instant] the server stored.
 */
@Serializable
data class NoteResponse(
    val id: String,
    val text: String,
    val authorEmail: String,
    val createdAt: Instant,
)

/**
 * The list payload. A wrapper (rather than a bare array) leaves room to add paging/cursors later
 * without breaking the wire shape — an old client tolerates the new fields (`ignoreUnknownKeys`).
 */
@Serializable data class NoteListResponse(val notes: List<NoteResponse>)
