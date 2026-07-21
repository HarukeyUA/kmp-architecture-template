package org.example.project.shared.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The single wire envelope every error response carries: the [error] (an [ApiError] encoded through
 * its sealed lens — see [encodeApiError] / [decodeDeclaredApiError]) plus the request correlation
 * id (the same id that appears in the server's structured logs, ADR-0005), so a user-facing failure
 * can be traced to its log line. The field is raw JSON rather than a typed [ApiError] because
 * narrowing needs the operation's lens, which only the call site holds.
 */
@Serializable
data class ErrorEnvelope(
    @SerialName("error") val error: JsonObject,
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
