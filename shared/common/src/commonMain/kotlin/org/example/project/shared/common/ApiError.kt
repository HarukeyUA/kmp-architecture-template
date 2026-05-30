package org.example.project.shared.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The wire error taxonomy crossing the Seam — an **open polymorphic** base so a newer server's
 * error variant degrades to [UnknownApiError] on an older, un-updatable client instead of crashing
 * it (ADR-0005).
 *
 * Cross-cutting variants live here in `:shared:common`; per-domain variants are declared directly
 * as `: ApiError` in their own `:shared:<domain>` (interim stop-gap — no sealed grouping yet, see
 * ADR-0005). Each `@SerialName` is namespaced (`common.*`, `notes.*`, …) and frozen by a golden-set
 * test. `ApiError` is the information-disclosure boundary: e.g. unknown-user and wrong-password
 * both collapse to [Unauthorized].
 */
interface ApiError

@Serializable @SerialName("common.unauthorized") data object Unauthorized : ApiError

@Serializable @SerialName("common.forbidden") data object Forbidden : ApiError

@Serializable @SerialName("common.not_found") data class NotFound(val resource: String) : ApiError

@Serializable
@SerialName("common.conflict")
data class Conflict(val reason: String? = null) : ApiError

@Serializable
@SerialName("common.validation")
data class Validation(val fields: List<FieldError>) : ApiError

@Serializable
@SerialName("common.rate_limited")
data class RateLimited(val retryAfterSeconds: Long? = null) : ApiError

@Serializable @SerialName("common.internal") data object Internal : ApiError

/**
 * Forward-compat fallback: any discriminator the client doesn't recognise deserializes here (via
 * the default deserializer in [commonApiErrorSerializersModule]) rather than throwing. [code] is
 * the unrecognised `@SerialName`; [raw] preserves the original JSON for diagnostics/logging.
 */
data class UnknownApiError(val code: String, val raw: JsonObject? = null) : ApiError
