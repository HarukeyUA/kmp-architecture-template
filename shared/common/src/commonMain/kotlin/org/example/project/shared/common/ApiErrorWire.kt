package org.example.project.shared.common

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 * The seam's [Json] — one static configuration, **client and server both use this instance**, so
 * the wire format is identical on both ends. Errors need no registered modules: each side
 * serializes them through a sealed lens ([Endpoint.error] or [commonApiErrorSerializer]) rather
 * than open polymorphism.
 *
 * Forward-compatible by default — `ignoreUnknownKeys` and `explicitNulls = false` let an old client
 * tolerate new fields a newer server sends.
 */
val seamJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/** The sealed lens over the cross-cutting [CommonApiError] variants. */
val commonApiErrorSerializer: KSerializer<CommonApiError> = serializer()

/** Encodes [error] through its sealed [lens] into the [ErrorEnvelope.error] wire shape. */
fun <E : ApiError> encodeApiError(
    lens: KSerializer<E>,
    error: E,
    json: Json = seamJson,
): JsonObject = json.encodeToJsonElement(lens, error).jsonObject

/**
 * Decodes [raw] through an operation's Declared [lens], or returns null when the carried code is
 * outside the lens — the caller then falls back to [decodeAmbientApiError]. Decode success *is* the
 * Declared/Ambient narrowing; there is no separate instance check.
 */
fun <E : ApiError> decodeDeclaredApiError(
    lens: KSerializer<E>,
    raw: JsonObject,
    json: Json = seamJson,
): E? =
    try {
        json.decodeFromJsonElement(lens, raw)
    } catch (_: SerializationException) {
        null
    }

/**
 * Decodes [raw] as a cross-cutting [CommonApiError], degrading to [UnknownApiError] (carrying the
 * unrecognised code and the raw JSON) so an old client never crashes on a newer server's error
 * code. [status] is the response's actual HTTP status; a recognised variant keeps its declared
 * status, an unrecognised one carries [status] since the response line is known even when the body
 * code isn't.
 */
fun decodeAmbientApiError(
    raw: JsonObject,
    status: HttpStatusCode,
    json: Json = seamJson,
): ApiError =
    decodeDeclaredApiError(commonApiErrorSerializer, raw, json)
        ?: UnknownApiError(code = raw.discriminator(json) ?: "unknown", raw = raw, status = status)

private fun JsonObject.discriminator(json: Json): String? =
    this[json.configuration.classDiscriminator]?.jsonPrimitive?.contentOrNull
