package org.example.project.shared.notes

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlin.jvm.JvmInline
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.ValidationError

/**
 * Pure **shape** validation reused on both sides: the client runs it for instant feedback, the
 * server runs the same code as its security boundary. The per-account character **quota** is
 * deliberately *not* here — it needs the DB, so it's server-only (the `NotesService` raises
 * [NotesQuotaExceeded]).
 *
 * Length is measured in **Unicode code points** — the one unit countable in both common Kotlin and
 * Postgres (`char_length`), so this cap, the server quota, and the stored sum all agree. (Code
 * points still aren't user-perceived characters — a ZWJ emoji sequence is one glyph but several
 * code points; grapheme clusters aren't cheaply countable on either side, so we don't pretend.)
 */
@JvmInline
value class NoteText private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 1_000

        fun of(raw: String): Either<FieldError, NoteText> = either {
            ensure(raw.isNotBlank()) { ValidationError.field("text", "blank") }
            ensure(raw.codePointLength() <= MAX_LENGTH) {
                ValidationError.field("text", "too_long")
            }
            NoteText(raw)
        }

        /** Code points, not UTF-16 units: a low surrogate is the second half of one code point. */
        private fun String.codePointLength(): Int = count { !it.isLowSurrogate() }
    }
}
