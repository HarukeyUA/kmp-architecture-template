package org.example.project.shared.tasks

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.shared.common.FieldError

class TasksValidationTest {
    @Test
    fun `blank title is rejected`() {
        assertThat(TaskTitle.of(" \n\t ").leftOrNull()).isEqualTo(FieldError("title", "blank"))
    }

    @Test
    fun `title is trimmed then accepted`() {
        assertThat(TaskTitle.of("  Buy milk ").getOrNull()?.value).isEqualTo("Buy milk")
    }

    @Test
    fun `length limit applies to the trimmed title`() {
        val longest = "a".repeat(TaskTitle.MAX_LENGTH)

        assertThat(TaskTitle.of(" $longest ").getOrNull()?.value).isEqualTo(longest)
        assertThat(TaskTitle.of(longest + "a").leftOrNull())
            .isEqualTo(FieldError("title", "too_long"))
    }

    @Test
    fun `NUL is rejected because Postgres cannot store it`() {
        assertThat(TaskTitle.of("a\u0000b").leftOrNull()).isEqualTo(FieldError("title", "format"))
    }
}
