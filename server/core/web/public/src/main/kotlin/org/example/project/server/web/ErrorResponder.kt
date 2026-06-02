package org.example.project.server.web

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.BadRequest
import org.example.project.shared.common.Conflict
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.Forbidden
import org.example.project.shared.common.Internal
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.RateLimited
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.UnknownApiError
import org.example.project.shared.common.Validation

fun interface ApiErrorStatusMapper {
    fun statusFor(error: ApiError): HttpStatusCode?
}

private val ApiErrorStatusMappersKey: AttributeKey<Set<ApiErrorStatusMapper>> =
    AttributeKey("ApiErrorStatusMappers")

fun Application.installApiErrorStatusMappers(mappers: Set<ApiErrorStatusMapper>) {
    attributes.put(ApiErrorStatusMappersKey, mappers)
}

/**
 * The ONE place an [ApiError] meets HTTP (ADR-0005). Services return `Either<ApiError, T>` and stay
 * HTTP-agnostic; the route maps the semantic error to a status here. Domain-specific errors that
 * don't reuse a cross-cutting variant may contribute an [ApiErrorStatusMapper]; otherwise they fall
 * to `400` — the precise variant still rides in the body, which is what the client matches on.
 */
fun ApiError.toStatus(mappers: Set<ApiErrorStatusMapper> = emptySet()): HttpStatusCode =
    mappers.firstNotNullOfOrNull { it.statusFor(this) } ?: toDefaultStatus()

private fun ApiError.toDefaultStatus(): HttpStatusCode =
    when (this) {
        is Validation -> HttpStatusCode.UnprocessableEntity
        Unauthorized -> HttpStatusCode.Unauthorized
        Forbidden -> HttpStatusCode.Forbidden
        is BadRequest -> HttpStatusCode.BadRequest
        is NotFound -> HttpStatusCode.NotFound
        is Conflict -> HttpStatusCode.Conflict
        is RateLimited -> HttpStatusCode.TooManyRequests
        Internal,
        is UnknownApiError -> HttpStatusCode.InternalServerError
        else -> HttpStatusCode.BadRequest
    }

/**
 * Responds with [error] mapped to its status, wrapped in an [ErrorEnvelope] carrying the request
 * id.
 */
suspend fun ApplicationCall.respondError(error: ApiError) {
    val mappers = application.attributes.getOrNull(ApiErrorStatusMappersKey).orEmpty()
    respond(error.toStatus(mappers), ErrorEnvelope(error = error, requestId = callId))
}

/**
 * Folds an `Either<ApiError, T>` from the service into an HTTP response: left → [respondError];
 * right → [onSuccess] (which chooses the success status, e.g. `201 Created`).
 */
suspend fun <T> ApplicationCall.respondEither(
    result: Either<ApiError, T>,
    onSuccess: suspend ApplicationCall.(T) -> Unit,
) {
    result.fold(ifLeft = { respondError(it) }, ifRight = { onSuccess(it) })
}
