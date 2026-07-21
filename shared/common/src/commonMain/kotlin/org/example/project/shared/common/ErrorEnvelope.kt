package org.example.project.shared.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The single wire envelope every error response carries: the typed [error] plus the request
 * correlation id (the same id that appears in the server's structured logs, ADR-0005), so a
 * user-facing failure can be traced to its log line.
 */
@Serializable
data class ErrorEnvelope(
    @SerialName("error") val error: ApiError,
    @SerialName("requestId") val requestId: String? = null,
)

/** One field's shape-validation failure. [code] is a stable machine code the client localizes. */
@Serializable
data class FieldError(@SerialName("field") val field: String, @SerialName("code") val code: String)

/**
 * Factory for shape-validation failures. Shared smart constructors return `Either<FieldError, T>`
 * and raise via `ValidationError.field(...)`; the route layer collects them into a [Validation].
 */
object ValidationError {
    fun field(name: String, code: String): FieldError = FieldError(field = name, code = code)
}
