package org.example.project.shared.common

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The wire error taxonomy crossing the Seam. [status] is the HTTP status the server answers with —
 * it is part of the wire contract (the client parses an `ErrorEnvelope` only out of a 4xx), so it
 * is declared on the error itself: declaring a variant and mapping its status cannot drift apart.
 *
 * Cross-cutting variants live under [CommonApiError]; per-domain variants implement their
 * operation's Declared-error lens (a `@Serializable sealed interface`) in their own
 * `:shared:<domain>`. Each `@SerialName` is namespaced (`common.*`, `auth.*`, …) and frozen by a
 * golden test. `ApiError` is the information-disclosure boundary: e.g. unknown-user and
 * wrong-password both collapse to [Unauthorized].
 */
interface ApiError {
    val status: HttpStatusCode
}

/**
 * The cross-cutting variants any endpoint may return regardless of its Declared lens. Sealed so
 * both ends encode/decode them through the compiler-generated lens serializer — no hand-kept
 * registry. A wire code outside every lens and outside this set decodes to [UnknownApiError].
 */
@Serializable sealed interface CommonApiError : ApiError

@Serializable
@SerialName("common.unauthorized")
data object Unauthorized : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Unauthorized
}

@Serializable
@SerialName("common.forbidden")
data object Forbidden : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Forbidden
}

@Serializable
@SerialName("common.bad_request")
data class BadRequest(@SerialName("reason") val reason: String? = null) : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.BadRequest
}

@Serializable
@SerialName("common.not_found")
data class NotFound(@SerialName("resource") val resource: String) : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.NotFound
}

@Serializable
@SerialName("common.conflict")
data class Conflict(@SerialName("reason") val reason: String? = null) : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Conflict
}

@Serializable
@SerialName("common.validation")
data class Validation(@SerialName("fields") val fields: List<FieldError>) : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.UnprocessableEntity
}

@Serializable
@SerialName("common.rate_limited")
data class RateLimited(@SerialName("retryAfterSeconds") val retryAfterSeconds: Long? = null) :
    CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.TooManyRequests
}

@Serializable
@SerialName("common.payload_too_large")
data object PayloadTooLarge : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.PayloadTooLarge
}

@Serializable
@SerialName("common.internal")
data object Internal : CommonApiError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.InternalServerError
}

/**
 * Forward-compat fallback: any discriminator the client doesn't recognise decodes here (via
 * [decodeAmbientApiError]) rather than throwing. [code] is the unrecognised `@SerialName`; [raw]
 * preserves the original JSON for diagnostics/logging. Client-side decode artifact only — the
 * server never constructs or sends one. [status] is the status the server actually answered with:
 * it rides the response line, not the body, so it is known even when the code isn't.
 */
data class UnknownApiError(
    val code: String,
    val raw: JsonObject? = null,
    override val status: HttpStatusCode,
) : ApiError
