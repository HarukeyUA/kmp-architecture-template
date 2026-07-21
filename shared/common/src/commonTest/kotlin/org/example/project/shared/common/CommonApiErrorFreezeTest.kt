package org.example.project.shared.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.json.jsonPrimitive

/**
 * Golden-set freeze for the cross-cutting `ApiError` codes *and* their HTTP statuses. The codes are
 * the wire contract: old, un-updatable clients match on them, so renaming one silently breaks every
 * shipped client; the status is likewise contractual (the client parses an `ErrorEnvelope` only out
 * of a 4xx). Change either deliberately, never casually. Each `:shared:<domain>` freezes its own
 * `<domain>.*` codes in a sibling `*DeclaredErrorFreezeTest`.
 */
class CommonApiErrorFreezeTest {
    // Hand-instantiated (not derived) because several variants carry required fields.
    private val samples: List<CommonApiError> =
        listOf(
            Unauthorized,
            Forbidden,
            BadRequest(),
            NotFound("x"),
            Conflict(),
            Validation(emptyList()),
            RateLimited(),
            PayloadTooLarge,
            Internal,
        )

    @Test
    fun `cross-cutting codes and statuses are frozen`() {
        val actual = samples.associate { it.wireCode() to it.status.value }

        assertThat(actual)
            .isEqualTo(
                mapOf(
                    "common.unauthorized" to 401,
                    "common.forbidden" to 403,
                    "common.bad_request" to 400,
                    "common.not_found" to 404,
                    "common.conflict" to 409,
                    "common.validation" to 422,
                    "common.rate_limited" to 429,
                    "common.payload_too_large" to 413,
                    "common.internal" to 500,
                )
            )
    }

    @Test
    fun `sample list covers every declared variant`() {
        // Keeps the freeze self-extending: a new CommonApiError variant fails here until a sample
        // (and thus a golden entry above) is added.
        val subclasses = commonApiErrorSerializer.descriptor.getElementDescriptor(1)
        val declared =
            (0 until subclasses.elementsCount)
                .map { subclasses.getElementDescriptor(it).serialName }
                .toSet()

        assertThat(samples.map { it.wireCode() }.toSet()).isEqualTo(declared)
    }

    private fun CommonApiError.wireCode(): String =
        encodeApiError(commonApiErrorSerializer, this).getValue("type").jsonPrimitive.content
}
