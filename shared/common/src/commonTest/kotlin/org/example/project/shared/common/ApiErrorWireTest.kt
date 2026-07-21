package org.example.project.shared.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/** A stand-in Declared lens, local so `:shared:common` needn't depend on a domain module. */
@Serializable private sealed interface TestLensError : ApiError

@Serializable
@SerialName("test.teapot")
private data object Teapot : TestLensError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.fromValue(418)
}

private val testLens: KSerializer<TestLensError> = serializer()

/**
 * Proves the sealed-lens wire mechanism the whole error pipeline rides on: encode through a lens,
 * decode-to-narrow, and the forward-compat fallback.
 */
class ApiErrorWireTest {
    @Test
    fun `every cross-cutting variant round-trips through the common lens`() {
        val cases: List<CommonApiError> =
            listOf(
                Unauthorized,
                Forbidden,
                BadRequest("malformed_body"),
                NotFound("note"),
                Conflict("already exists"),
                Validation(listOf(FieldError("email", "format"))),
                RateLimited(retryAfterSeconds = 30),
                PayloadTooLarge,
                Internal,
            )

        cases.forEach { original ->
            val decoded =
                decodeAmbientApiError(
                    encodeApiError(commonApiErrorSerializer, original),
                    status = HttpStatusCode.BadRequest,
                )
            assertThat(decoded).isEqualTo(original)
        }
    }

    @Test
    fun `a declared code decodes through its lens`() {
        val raw = encodeApiError(testLens, Teapot)

        assertThat(decodeDeclaredApiError(testLens, raw)).isEqualTo(Teapot as TestLensError)
    }

    @Test
    fun `a code outside the lens falls out of the declared decode`() {
        // A cross-cutting code arriving on an endpoint whose lens doesn't declare it — the
        // version-skew / contract-drift case. It must fall to the Ambient path, not decode.
        val raw = encodeApiError(commonApiErrorSerializer, Unauthorized)

        assertThat(decodeDeclaredApiError(testLens, raw)).isNull()
        assertThat(decodeAmbientApiError(raw, status = HttpStatusCode.BadRequest))
            .isEqualTo(Unauthorized as ApiError)
    }

    @Test
    fun `an unknown discriminator degrades to UnknownApiError instead of throwing`() {
        // Simulates a newer server sending an error code this client was not built against.
        val fromFutureServer =
            seamJson.decodeFromString<ErrorEnvelope>(
                """{"error":{"type":"future.teapot","brewing":true}}"""
            )

        val decoded =
            decodeAmbientApiError(fromFutureServer.error, status = HttpStatusCode.fromValue(418))

        assertThat(decoded).isInstanceOf(UnknownApiError::class)
        val unknown = decoded as UnknownApiError
        assertThat(unknown.code).isEqualTo("future.teapot")
        assertThat(unknown.raw).isNotNull()
        assertThat(unknown.raw!!["brewing"]!!.jsonPrimitive.content).isEqualTo("true")
        // The response line's status rides along even though the body's code is unknown.
        assertThat(unknown.status.value).isEqualTo(418)
    }

    @Test
    fun `envelope round-trips a lens-encoded error with its request id`() {
        val envelope =
            ErrorEnvelope(
                error = encodeApiError(commonApiErrorSerializer, NotFound("note")),
                requestId = "req-42",
            )

        val decoded = seamJson.decodeFromString<ErrorEnvelope>(seamJson.encodeToString(envelope))

        assertThat(decoded).isEqualTo(envelope)
        assertThat(decoded.error["type"]).isEqualTo(JsonPrimitive("common.not_found"))
        assertThat(decodeAmbientApiError(decoded.error, status = HttpStatusCode.NotFound))
            .isEqualTo(NotFound("note") as ApiError)
    }

    @Test
    fun `an endpoint rejects a lens that is not a sealed serializer`() {
        // Forgetting @Serializable on a lens silently yields an OPEN polymorphic serializer —
        // every Declared error would ride Ambient. The Endpoint constructor is the tripwire.
        assertFailsWith<IllegalArgumentException> {
            Endpoint<Any, Unit, Unit, ApiError>(
                HttpMethod.Get,
                request = null,
                response = null,
                error = PolymorphicSerializer(ApiError::class),
            )
        }
    }
}
