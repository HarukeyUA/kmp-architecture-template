package org.example.project.shared.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * Golden-set freeze for the cross-cutting `ApiError` `@SerialName`s (ADR-0005). These codes are the
 * wire contract: old, un-updatable clients match on them, so renaming one silently breaks every
 * shipped client. Changing a name here is a breaking change — update deliberately, never casually.
 * Each `:shared:<domain>` adds its own sibling golden test for its `<domain>.*` codes.
 */
@OptIn(ExperimentalSerializationApi::class)
class ApiErrorSerialNameGoldenTest {
    @Test
    fun `cross-cutting error codes are frozen`() {
        val frozen =
            mapOf(
                serializer<Unauthorized>().descriptor.serialName to "common.unauthorized",
                serializer<Forbidden>().descriptor.serialName to "common.forbidden",
                serializer<BadRequest>().descriptor.serialName to "common.bad_request",
                serializer<NotFound>().descriptor.serialName to "common.not_found",
                serializer<Conflict>().descriptor.serialName to "common.conflict",
                serializer<Validation>().descriptor.serialName to "common.validation",
                serializer<RateLimited>().descriptor.serialName to "common.rate_limited",
                serializer<Internal>().descriptor.serialName to "common.internal",
            )

        frozen.forEach { (actual, expected) -> assertThat(actual).isEqualTo(expected) }
    }
}
