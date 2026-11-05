package org.example.project.feature.tasks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import org.example.project.client.feature.tasks.impl.Res
import org.example.project.client.feature.tasks.impl.tasks_error_not_found
import org.example.project.client.feature.tasks.impl.tasks_error_title_invalid
import org.example.project.core.error.CallFailure
import org.example.project.core.ui.error.ErrorRenderer
import org.example.project.core.ui.error.ErrorRenderer.ResourceResult
import org.example.project.shared.tasks.TaskNotFound
import org.example.project.shared.tasks.TaskValidationFailed

/** The operations' Declared errors, which the shared renderers leave to the owning feature. */
@Inject
@ContributesIntoSet(AppScope::class)
class TasksErrorRenderer : ErrorRenderer<CallFailure<*>> {
    override fun resolveResource(error: CallFailure<*>): ResourceResult? =
        when ((error as? CallFailure.Declared)?.error) {
            TaskNotFound -> ResourceResult(Res.string.tasks_error_not_found)
            is TaskValidationFailed -> ResourceResult(Res.string.tasks_error_title_invalid)
            else -> null
        }
}
