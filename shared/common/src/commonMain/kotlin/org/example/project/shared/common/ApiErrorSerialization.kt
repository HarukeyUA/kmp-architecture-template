package org.example.project.shared.common

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Base [SerializersModule] registering the cross-cutting [ApiError] variants and the forward-compat
 * default deserializer.
 *
 * Each `:shared:<domain>` exposes its own module registering its variants; each side's `:impl`
 * contributes it via Metro `@ContributesIntoSet`, and `:client:core:network` / `:server:core:web`
 * fold the multibound `Set<SerializersModule>` + this base into one `Json`. kotlinx throws on a
 * duplicate `@SerialName` when modules are combined, giving global uniqueness for free.
 */
val commonApiErrorSerializersModule: SerializersModule = SerializersModule {
    polymorphic(ApiError::class) {
        subclass(Unauthorized::class)
        subclass(Forbidden::class)
        subclass(BadRequest::class)
        subclass(NotFound::class)
        subclass(Conflict::class)
        subclass(Validation::class)
        subclass(RateLimited::class)
        subclass(Internal::class)
        defaultDeserializer { discriminator -> UnknownApiErrorDeserializer(discriminator) }
    }
}

/**
 * Captures an unrecognised `ApiError` discriminator (and the raw JSON) into [UnknownApiError] so an
 * old client never crashes on a new server's error code. JSON-only by design (the seam is JSON).
 */
private class UnknownApiErrorDeserializer(private val discriminator: String?) :
    DeserializationStrategy<UnknownApiError> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.example.project.shared.common.UnknownApiError")

    override fun deserialize(decoder: Decoder): UnknownApiError {
        val raw = (decoder as? JsonDecoder)?.decodeJsonElement() as? JsonObject
        return UnknownApiError(code = discriminator ?: "unknown", raw = raw)
    }
}
