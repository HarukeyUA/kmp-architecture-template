package org.example.project.feature.tasks

import org.example.project.core.component.AppComponentContext
import org.example.project.core.component.StatefulComponent
import org.example.project.core.component.UiState

/** The tasks tab: the list, plus the open task and the create form when they are up. */
interface TasksComponent : StatefulComponent<TasksComponent.State> {

    data class State(
        val list: TaskListComponent.State,
        val detail: TaskDetailComponent.State? = null,
        val create: CreateTaskComponent.State? = null,
    ) : UiState

    fun interface Factory {
        fun create(componentContext: AppComponentContext): TasksComponent
    }
}
