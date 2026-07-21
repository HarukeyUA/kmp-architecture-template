package org.example.project.core.error

/**
 * Transport-level failure of a remote call: no typed wire error was produced. A parsed wire
 * [ApiError][org.example.project.shared.common.ApiError] is carried by
 * [CallFailure.Ambient]/[CallFailure.Declared] instead, not by this hierarchy (ADR-0011).
 */
sealed interface NetworkError : AppError {
    /** HTTP error with a status code and optional error message from the server. */
    data class Http(val code: Int, val message: String? = null) : NetworkError

    /** Network-level failure (DNS, timeout, connection refused, etc.). */
    data class Connection(val cause: Throwable) : NetworkError

    /** Failed to serialize the request or deserialize the response. */
    data class Serialization(val cause: Throwable) : NetworkError
}
