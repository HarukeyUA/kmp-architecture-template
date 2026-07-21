package org.example.project.server

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import org.example.project.shared.auth.AuthApi
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.commonApiErrorSerializer
import org.example.project.shared.notes.NotesApi

/**
 * Every wire error code must belong to exactly one error class across all domains and the common
 * set. The old combined `SerializersModule` threw on a duplicate `@SerialName` at fold time; with
 * sealed-lens serialization (ADR-0012) nothing combines the domains at runtime, so this test — in
 * the one module that sees every `:shared:<domain>` — replaces that guarantee. A duplicate would
 * make decode ambiguous for clients matching on codes.
 *
 * One class appearing in several lenses is a single declaration, so codes are keyed by the
 * variant's descriptor: a collision is one code claimed by two *distinct* descriptors.
 */
class UniqueErrorCodesTest {
    @Test
    fun `error codes are unique across all domains and the common set`() {
        val lenses: List<KSerializer<out ApiError>?> =
            listOf(
                commonApiErrorSerializer,
                AuthApi.signup.error,
                AuthApi.login.error,
                AuthApi.refresh.error,
                NotesApi.create.error,
            )

        val descriptorsPerCode = mutableMapOf<String, MutableSet<SerialDescriptor>>()
        lenses.filterNotNull().forEach { lens ->
            val subclasses = lens.descriptor.getElementDescriptor(1)
            (0 until subclasses.elementsCount)
                .map { subclasses.getElementDescriptor(it) }
                .forEach { descriptorsPerCode.getOrPut(it.serialName) { mutableSetOf() }.add(it) }
        }

        val collisions = descriptorsPerCode.filterValues { it.size > 1 }.keys
        assertThat(collisions).isEqualTo(emptySet<String>())
    }
}
