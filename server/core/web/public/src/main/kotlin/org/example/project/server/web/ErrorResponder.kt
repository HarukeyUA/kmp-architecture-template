package org.example.project.server.web

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.CommonApiError
import org.example.project.shared.common.ErrorEnvelope
import org.example.project.shared.common.Internal
import org.example.project.shared.common.commonApiErrorSerializer
import org.example.project.shared.common.encodeApiError

/**
 * The ONE place an [ApiError] meets HTTP. Services return `Either<Failure<Err>, T>` and stay
 * HTTP-agnostic; here the error's own declared [ApiError.status] answers, and the body is the error
 * encoded through its sealed lens (the endpoint's Declared lens, or [CommonApiError] for
 * cross-cutting failures) into the [ErrorEnvelope]. An error that is neither — a foreign domain's
 * error smuggled into the Ambient arm — is by definition a server fault and answers [Internal].
 */
suspend fun ApplicationCall.respondError(error: CommonApiError) {
    respondEncoded(error, encodeApiError(commonApiErrorSerializer, error))
}

/**
 * Folds an `Either<Failure<Err>, T>` from the service into an HTTP response: left → the error's own
 * status + lens-encoded envelope; right → [onSuccess] (which chooses the success status, e.g. `201
 * Created`). [lens] is the endpoint's Declared lens (`Endpoint.error`); `null` means the endpoint
 * declares no errors, so only Ambient failures can arrive.
 */
suspend fun <Err : ApiError, T> ApplicationCall.respondEither(
    lens: KSerializer<Err>?,
    result: Either<Failure<Err>, T>,
    onSuccess: suspend ApplicationCall.(T) -> Unit,
) {
    result.fold(ifLeft = { respondFailure(lens, it) }, ifRight = { onSuccess(it) })
}

private suspend fun <Err : ApiError> ApplicationCall.respondFailure(
    lens: KSerializer<Err>?,
    failure: Failure<Err>,
) {
    when (failure) {
        is Failure.Declared ->
            if (lens != null) respondEncoded(failure.error, encodeApiError(lens, failure.error))
            else respondUnencodable(failure.error)
        is Failure.Ambient ->
            when (val error = failure.error) {
                is CommonApiError -> respondError(error)
                else -> respondUnencodable(error)
            }
    }
}

/**
 * An error that reached HTTP without a lens that can encode it — unanticipated by construction, so
 * it answers `500` (a server fault, not `400`) and is logged so the misrouted error is
 * discoverable.
 */
private suspend fun ApplicationCall.respondUnencodable(error: ApiError) {
    application.log.warn(
        "ApiError {} is neither this endpoint's Declared error nor a CommonApiError; " +
            "answering {}. Route it through the endpoint's lens or a cross-cutting variant.",
        error::class.simpleName,
        Internal.status,
    )
    respondError(Internal)
}

private suspend fun ApplicationCall.respondEncoded(error: ApiError, encoded: JsonObject) {
    respond(error.status, ErrorEnvelope(error = encoded, requestId = callId))
}
