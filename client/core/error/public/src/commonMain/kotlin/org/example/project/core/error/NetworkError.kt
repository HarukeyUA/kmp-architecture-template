package org.example.project.core.error

import org.example.project.shared.common.ApiError

/** Typed error hierarchy for remote API operations. */
sealed interface NetworkError : AppError {
    /** HTTP error with a status code and optional error message from the server. */
    data class Http(val code: Int, val message: String? = null) : NetworkError

    /** Network-level failure (DNS, timeout, connection refused, etc.). */
    data class Connection(val cause: Throwable) : NetworkError

    /** Failed to serialize the request or deserialize the response. */
    data class Serialization(val cause: Throwable) : NetworkError

    /**
     * A typed [ApiError] parsed from a 4xx error envelope. Keeps the pure shared error out of the
     * client's `AppError` hierarchy (umbrella law) while still carrying it to the renderer
     * (ADR-0005).
     */
    data class Api(val error: ApiError) : NetworkError
}
