package org.example.project.server.web

import arrow.core.raise.Raise
import org.example.project.shared.common.ApiError

/**
 * The server-side two-arm outcome of a handler or service (ADR-0011). A [Declared] error is one the
 * endpoint commits to in its `Endpoint.error` set: the compiler only lets a service put an error
 * here when it is assignable to the endpoint's [E], so a service *cannot* declare an error the
 * endpoint didn't — the client's exhaustive `when` is guaranteed, not hoped for. An [Ambient] error
 * is a cross-cutting `:shared:common` failure (validation, not-found, unauthorized…) handled once,
 * centrally; it is `Failure<Nothing>`, so it rides any endpoint's channel regardless of what that
 * endpoint declares.
 *
 * Both arms erase to a plain [ApiError] at the HTTP boundary: [Route.serve] reads [error] and the
 * existing `ErrorResponder`/status-mapping path is untouched. The wrapper is a compile-time guard,
 * not a wire type.
 */
sealed interface Failure<out E : ApiError> {
    val error: ApiError

    data class Declared<out E : ApiError>(override val error: E) : Failure<E>

    data class Ambient(override val error: ApiError) : Failure<Nothing>
}

/**
 * Raises [error] in the Declared channel. Type-checks only when [error] is assignable to the
 * endpoint's declared [E] — the enforcement point ADR-0011 relies on.
 */
fun <E : ApiError> Raise<Failure<E>>.declared(error: E): Nothing = raise(Failure.Declared(error))

/** Raises a cross-cutting [error] Ambiently; valid in any endpoint's channel (see [Failure]). */
fun <E : ApiError> Raise<Failure<E>>.ambient(error: ApiError): Nothing =
    raise(Failure.Ambient(error))
