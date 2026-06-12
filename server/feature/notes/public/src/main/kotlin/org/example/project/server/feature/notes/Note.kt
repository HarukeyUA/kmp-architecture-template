package org.example.project.server.feature.notes

import kotlin.time.Instant
import org.example.project.server.auth.AccountId

/**
 * The notes domain model — what the repository persists and returns, never an Exposed `ResultRow`
 * (ADR-0006). Lives in `:public` because the service returns it across module boundaries
 * (ADR-0003 as amended).
 */
data class Note(val id: String, val accountId: AccountId, val text: String, val createdAt: Instant)

/**
 * The read-model the notes use cases produce: a [Note] together with its author's identity,
 * resolved through the auth domain's public service. Routes map this to the wire `NoteResponse`.
 */
data class AuthoredNote(val note: Note, val authorEmail: String)
