package org.example.project.shared.notes

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlin.jvm.JvmInline
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.ValidationError

/**
 * Pure **shape** validation reused on both sides (ADR-0004): the client runs it for instant
 * feedback, the server runs the same code as its security boundary. The per-account character
 * **quota** is deliberately *not* here — it needs the DB, so it's server-only (the `NotesService`
 * raises [NotesQuotaExceeded]).
 */
@JvmInline
value class NoteText private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 10_000

        fun of(raw: String): Either<FieldError, NoteText> = either {
            ensure(raw.isNotBlank()) { ValidationError.field("text", "blank") }
            ensure(raw.length <= MAX_LENGTH) { ValidationError.field("text", "too_long") }
            NoteText(raw)
        }
    }
}
