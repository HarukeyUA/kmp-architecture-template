package org.example.project.feature.tasks

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.tasks_error_not_found
import org.example.project.client.feature.tasks.impl.tasks_error_title_invalid
import org.example.project.core.error.AppError
import org.example.project.core.error.CallFailure
import org.example.project.core.ui.error.CallFailureRenderer
import org.example.project.core.ui.error.CompositeErrorRenderer
import org.example.project.core.ui.error.ErrorRenderer
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskValidationFailed

class TasksErrorRendererTest {
    @Suppress("UNCHECKED_CAST")
    private val renderer =
        CompositeErrorRenderer(
            setOf(CallFailureRenderer(), TasksErrorRenderer()) as Set<ErrorRenderer<AppError>>,
        )

    @Test
    fun `a task that is gone is named, past the shared renderers`() {
        assertThat(renderer.resolveResource(CallFailure.Declared(TaskNotFound))?.resource)
            .isEqualTo(Res.string.tasks_error_not_found)
    }

    @Test
    fun `a refused title is named`() {
        val failure = CallFailure.Declared(TaskValidationFailed(emptyList()))

        assertThat(renderer.resolveResource(failure)?.resource)
            .isEqualTo(Res.string.tasks_error_title_invalid)
    }
}
