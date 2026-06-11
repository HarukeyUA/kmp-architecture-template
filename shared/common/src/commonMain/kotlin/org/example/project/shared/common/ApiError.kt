package org.example.project.shared.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The wire error taxonomy crossing the Seam — an **open polymorphic** base so a newer server's
 * error variant degrades to [UnknownApiError] on an older, un-updatable client instead of crashing
 * it.
 *
 * Cross-cutting variants live here in `:shared:common`; per-domain variants are declared directly
 * as `: ApiError` in their own `:shared:<domain>`. Each `@SerialName` is namespaced (`common.*`,
 * `notes.*`, …) and frozen by a golden-set test. `ApiError` is the information-disclosure boundary:
 * e.g. unknown-user and wrong-password both collapse to [Unauthorized].
 */
interface ApiError

@Serializable @SerialName("common.unauthorized") data object Unauthorized : ApiError

@Serializable @SerialName("common.forbidden") data object Forbidden : ApiError

@Serializable
@SerialName("common.bad_request")
data class BadRequest(@SerialName("reason") val reason: String? = null) : ApiError

@Serializable
@SerialName("common.not_found")
data class NotFound(@SerialName("resource") val resource: String) : ApiError

@Serializable
@SerialName("common.conflict")
data class Conflict(@SerialName("reason") val reason: String? = null) : ApiError

@Serializable
@SerialName("common.validation")
data class Validation(@SerialName("fields") val fields: List<FieldError>) : ApiError

@Serializable
@SerialName("common.rate_limited")
data class RateLimited(@SerialName("retryAfterSeconds") val retryAfterSeconds: Long? = null) :
    ApiError

@Serializable @SerialName("common.payload_too_large") data object PayloadTooLarge : ApiError

@Serializable @SerialName("common.internal") data object Internal : ApiError

/**
 * Forward-compat fallback: any discriminator the client doesn't recognise deserializes here (via
 * the default deserializer in [commonApiErrorSerializersModule]) rather than throwing. [code] is
 * the unrecognised `@SerialName`; [raw] preserves the original JSON for diagnostics/logging.
 */
data class UnknownApiError(val code: String, val raw: JsonObject? = null) : ApiError
