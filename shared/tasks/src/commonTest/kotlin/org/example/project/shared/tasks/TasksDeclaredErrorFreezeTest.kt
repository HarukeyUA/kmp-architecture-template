package org.example.project.shared.tasks

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.FieldError
import org.example.project.shared.common.encodeApiError

/** Codes and statuses are the wire contract for clients that cannot be updated. */
class TasksDeclaredErrorFreezeTest {
    private val getSamples: List<TasksGetError> = listOf(TaskNotFound)
    private val createSamples: List<TasksCreateError> =
        listOf(TaskValidationFailed(listOf(FieldError("title", "blank"))))
    private val setDoneSamples: List<TasksSetDoneError> = listOf(TaskNotFound)
    private val deleteSamples: List<TasksDeleteError> = listOf(TaskNotFound)

    private val getContract
        get() = declaredContract(requireNotNull(TasksApi.get.error), getSamples)

    private val createContract
        get() = declaredContract(requireNotNull(TasksApi.create.error), createSamples)

    private val setDoneContract
        get() = declaredContract(requireNotNull(TasksApi.setDone.error), setDoneSamples)

    private val deleteContract
        get() = declaredContract(requireNotNull(TasksApi.delete.error), deleteSamples)

    @Test
    fun `tasks declared error contracts are frozen`() {
        val notFound = mapOf("tasks.not_found" to 404)

        assertThat(TasksApi.list.error.declaredCodes()).isEqualTo(emptySet<String>())
        assertThat(getContract).isEqualTo(notFound)
        assertThat(createContract).isEqualTo(mapOf("tasks.validation" to 422))
        assertThat(setDoneContract).isEqualTo(notFound)
        assertThat(deleteContract).isEqualTo(notFound)
    }

    @Test
    fun `declared statuses are client-visible 4xx`() {
        // The client parses an ErrorEnvelope only out of a 4xx; a Declared error outside that
        // range would silently sever its own typed channel.
        val contract = getContract + createContract + setDoneContract + deleteContract
        contract.forEach { (code, status) ->
            assertThat(status in 400..499, name = code).isEqualTo(true)
        }
    }
}

// Samples are hand-instantiated because a variant can carry required fields. They must cover the
// sealed descriptor, so a new variant fails here until a sample and a golden entry are added.
private fun <E : ApiError> declaredContract(
    lens: KSerializer<E>,
    samples: List<E>,
): Map<String, Int> {
    val sampled = samples.associate { sample ->
        val code = encodeApiError(lens, sample).getValue("type").jsonPrimitive.content
        code to sample.status.value
    }
    require(sampled.keys == lens.declaredCodes()) {
        "Samples ${sampled.keys} must cover the lens's variants ${lens.declaredCodes()}"
    }
    return sampled
}

private fun KSerializer<out ApiError>?.declaredCodes(): Set<String> {
    if (this == null) return emptySet()
    val subclasses = descriptor.getElementDescriptor(1)
    return (0 until subclasses.elementsCount)
        .map { subclasses.getElementDescriptor(it).serialName }
        .toSet()
}
