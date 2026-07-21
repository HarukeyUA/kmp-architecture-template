package org.example.project.shared.auth

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlin.jvm.JvmInline
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.ValidationError

/**
 * Pure **shape** validation reused on both sides (ADR-0004): the client runs it for instant
 * feedback, the server runs the same code as its security boundary. Anything needing state or
 * identity (email uniqueness, credential check) is server-only.
 */
@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 254
        // Deliberately permissive: a single `@` with non-space text either side and a dotted host.
        private val PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(raw: String): Either<FieldError, Email> = either {
            val normalized = raw.trim().lowercase()
            ensure(normalized.isNotBlank()) { ValidationError.field("email", "blank") }
            ensure(normalized.length <= MAX_LENGTH) { ValidationError.field("email", "too_long") }
            ensure(PATTERN.matches(normalized)) { ValidationError.field("email", "format") }
            Email(normalized)
        }
    }
}

@JvmInline
value class Password private constructor(val value: String) {
    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 128

        fun of(raw: String): Either<FieldError, Password> = either {
            ensure(raw.length >= MIN_LENGTH) { ValidationError.field("password", "too_short") }
            ensure(raw.length <= MAX_LENGTH) { ValidationError.field("password", "too_long") }
            Password(raw)
        }
    }
}
