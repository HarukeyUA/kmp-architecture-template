package org.example.project.shared.notes

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.encodeApiError

/**
 * Declared-contract freeze (ADR-0011): golden-dumps each notes operation → its Declared errors'
 * `@SerialName`s *and* HTTP statuses, both read off the endpoint's sealed lens. Adding a variant or
 * changing a status fails this test until the golden (and the sample list) is edited deliberately —
 * the codes and statuses are the wire contract for un-updatable clients.
 */
class NotesDeclaredErrorFreezeTest {
    // Hand-instantiated samples, one per variant; coverage against the lens's descriptor below.
    private val createSamples: List<NotesCreateError> =
        listOf(NotesQuotaExceeded(quota = 0, used = 0))

    @Test
    fun `notes declared error contracts are frozen`() {
        assertThat(NotesApi.list.error.declaredCodes()).isEqualTo(emptySet<String>())
        assertThat(declaredContract(NotesApi.create.error, createSamples))
            .isEqualTo(mapOf("notes.quota_exceeded" to 409))
        assertThat(NotesApi.delete.error.declaredCodes()).isEqualTo(emptySet<String>())
    }

    @Test
    fun `declared statuses are client-visible 4xx`() {
        // The client parses an ErrorEnvelope only out of a 4xx; a Declared error outside that
        // range would silently sever its own typed channel.
        declaredContract(NotesApi.create.error, createSamples).forEach { (code, status) ->
            assertThat(status in 400..499, name = code).isEqualTo(true)
        }
    }
}

/**
 * `serialName → status` for a Declared lens from hand-instantiated [samples] (variants can carry
 * required fields, so instances aren't derivable). The sample list must cover every lens variant —
 * asserted against the sealed descriptor, so a new variant fails here until a sample (and thus a
 * golden entry) is added.
 */
private fun <E : ApiError> declaredContract(
    lens: KSerializer<E>?,
    samples: List<E>,
): Map<String, Int> {
    requireNotNull(lens)
    val sampled = samples.associate { sample ->
        val code = encodeApiError(lens, sample).getValue("type").jsonPrimitive.content
        code to sample.status.value
    }
    require(sampled.keys == lens.declaredCodes()) {
        "Samples ${sampled.keys} must cover the lens's variants ${lens.declaredCodes()}"
    }
    return sampled
}

/** Every variant `serialName` of a Declared lens, from its sealed descriptor. */
private fun KSerializer<out ApiError>?.declaredCodes(): Set<String> {
    if (this == null) return emptySet()
    val subclasses = descriptor.getElementDescriptor(1)
    return (0 until subclasses.elementsCount)
        .map { subclasses.getElementDescriptor(it).serialName }
        .toSet()
}
