package org.example.project.core.error

import org.example.project.shared.common.ApiError

/**
 * The typed outcome of a failed [Endpoint][org.example.project.shared.common.Endpoint] call
 * (ADR-0011): the client-side counterpart to the server's failure channel. The three arms mirror
 * the three ways a call can fail, and the [E] parameter carries the operation's Declared-error set
 * so a feature can branch on it exhaustively.
 *
 * - [Declared] holds an [E] the operation committed to (`is`-narrowed against the endpoint's
 *   `error` [kotlin.reflect.KClass]) — the feature handles these with an exhaustive `when`.
 * - [Ambient] holds a cross-cutting [ApiError] (`:shared:common`, or an
 *   `UnknownApiError`/undeclared code that failed the narrowing — the drift safety valve). Handled
 *   once, centrally, by the renderer pipeline.
 * - [Transport] holds a [NetworkError] (offline, 5xx, decode/parse failure) — no wire error was
 *   produced. It [delegates][DelegatingError] to that [NetworkError] so it rides the existing
 *   renderer path unchanged.
 *
 * When an operation Declares nothing, [E] is [Nothing]: [Declared] is statically unreachable and
 * only [Ambient]/[Transport] remain.
 */
sealed interface CallFailure<out E : ApiError> : AppError {
    data class Declared<E : ApiError>(val error: E) : CallFailure<E>

    data class Ambient(val error: ApiError) : CallFailure<Nothing>

    data class Transport(val error: NetworkError) : CallFailure<Nothing>, DelegatingError {
        override val delegate: AppError
            get() = error
    }
}
