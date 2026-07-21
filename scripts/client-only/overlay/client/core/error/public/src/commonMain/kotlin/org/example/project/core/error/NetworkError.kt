package org.example.project.core.error

/** Failure of a remote call. */
sealed interface NetworkError : AppError {
    /** HTTP error with a status code and optional error message from the server. */
    data class Http(val code: Int, val message: String? = null) : NetworkError

    /** Network-level failure (DNS, timeout, connection refused, etc.). */
    data class Connection(val cause: Throwable) : NetworkError

    /** Failed to serialize the request or deserialize the response. */
    data class Serialization(val cause: Throwable) : NetworkError
}
