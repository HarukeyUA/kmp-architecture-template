package org.example.project.shared.notes

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * Golden-set freeze for the notes domain's `@SerialName`s (ADR-0005), sibling to the auth and
 * cross-cutting freezes. These `notes.*` codes are the wire contract for un-updatable clients —
 * renaming one is a breaking change. Each domain owning its own golden test is how the freeze
 * "extends automatically" as domains are added.
 */
@OptIn(ExperimentalSerializationApi::class)
class NotesErrorGoldenTest {
    @Test
    fun `notes error codes are frozen`() {
        assertThat(serializer<NotesQuotaExceeded>().descriptor.serialName)
            .isEqualTo("notes.quota_exceeded")
    }
}
