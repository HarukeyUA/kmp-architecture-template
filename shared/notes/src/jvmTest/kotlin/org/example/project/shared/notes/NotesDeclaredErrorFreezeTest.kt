package org.example.project.shared.notes

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.example.project.shared.common.ApiError

/**
 * Declared-set freeze (ADR-0011): golden-dumps each notes operation → the `@SerialName`s of its
 * Declared errors, reached by walking the endpoint's error lens via `KClass.sealedSubclasses`. This
 * is the machine-checked answer to "what can this call return" — adding, removing, or re-pointing a
 * lens variant changes the dump and fails here, so the set can't drift silently. Lives in jvmTest
 * because `sealedSubclasses` is JVM-only reflection (kotlin-reflect); the golden-name freeze that
 * pins the codes themselves stays cross-platform in `NotesErrorGoldenTest`.
 */
@OptIn(InternalSerializationApi::class)
class NotesDeclaredErrorFreezeTest {
    @Test
    fun `notes declared error sets are frozen`() {
        val declared =
            mapOf(
                "list" to NotesApi.list.error.declaredDiscriminators(),
                "create" to NotesApi.create.error.declaredDiscriminators(),
                "delete" to NotesApi.delete.error.declaredDiscriminators(),
            )

        assertThat(declared)
            .isEqualTo(
                mapOf(
                    "list" to emptySet(),
                    "create" to setOf("notes.quota_exceeded"),
                    "delete" to emptySet(),
                )
            )
    }
}

@OptIn(InternalSerializationApi::class)
private fun KClass<out ApiError>?.declaredDiscriminators(): Set<String> =
    this?.sealedSubclasses.orEmpty().map { it.serializer().descriptor.serialName }.toSet()
