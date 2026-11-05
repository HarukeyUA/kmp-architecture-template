package org.example.project.core.error

/** Marker interface for all typed errors in the application. */
interface AppError

/**
 * An error that delegates to another error (e.g. a feature-specific error wrapping a network
 * error).
 */
interface DelegatingError : AppError {
    val delegate: AppError
}
