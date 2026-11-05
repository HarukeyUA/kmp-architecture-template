package org.example.project.shared.tasks

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlin.jvm.JvmInline
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.ValidationError

/** Shape validation both sides run: the client for feedback, the server as its boundary. */
@JvmInline
value class TaskTitle private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 200
        private const val FIELD = "title"
        private const val NUL = '\u0000'

        fun of(raw: String): Either<FieldError, TaskTitle> = either {
            val trimmed = raw.trim()
            ensure(trimmed.isNotEmpty()) { ValidationError.field(FIELD, "blank") }
            ensure(trimmed.length <= MAX_LENGTH) { ValidationError.field(FIELD, "too_long") }
            // Postgres TEXT cannot store NUL; without this it would surface as a 500.
            ensure(NUL !in trimmed) { ValidationError.field(FIELD, "format") }
            TaskTitle(trimmed)
        }
    }
}
