package org.example.project.shared.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlin.test.Test
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

/**
 * Proves the polymorphic `ApiError` mechanism the whole error pipeline rides on.
 *
 * Uses [PolymorphicSerializer] explicitly rather than the reified `serializer<ApiError>()`: on
 * Kotlin/Native (the client's iOS target) an interface serializer can't be resolved at runtime, so
 * the explicit constructor is the portable form. In production `ApiError` always travels inside
 * [ErrorEnvelope], whose generated serializer wires the polymorphic field at compile time on every
 * target — see [envelope round-trips a typed error].
 */
class ApiErrorSerializationTest {
    private val json = Json { serializersModule = commonApiErrorSerializersModule }
    private val apiError = PolymorphicSerializer(ApiError::class)

    @Test
    fun `every cross-cutting variant round-trips through the wire`() {
        val cases: List<ApiError> =
            listOf(
                Unauthorized,
                Forbidden,
                BadRequest("malformed_body"),
                NotFound("note"),
                Conflict("already exists"),
                Validation(listOf(FieldError("email", "format"))),
                RateLimited(retryAfterSeconds = 30),
                Internal,
            )

        cases.forEach { original ->
            val decoded = json.decodeFromString(apiError, json.encodeToString(apiError, original))
            assertThat(decoded).isEqualTo(original)
        }
    }

    @Test
    fun `an unknown discriminator degrades to UnknownApiError instead of throwing`() {
        // Simulates a newer server sending an error code this client was not built against.
        val fromFutureServer = """{"type":"future.teapot","brewing":true}"""

        val decoded = json.decodeFromString(apiError, fromFutureServer)

        assertThat(decoded).isInstanceOf(UnknownApiError::class)
        val unknown = decoded as UnknownApiError
        assertThat(unknown.code).isEqualTo("future.teapot")
        assertThat(unknown.raw).isNotNull()
    }

    @Test
    fun `envelope round-trips a typed error`() {
        val envelope = ErrorEnvelope(error = NotFound("note"), requestId = "req-42")

        val decoded = json.decodeFromString<ErrorEnvelope>(json.encodeToString(envelope))

        assertThat(decoded).isEqualTo(envelope)
    }
}
