package org.example.project.feature.tasks

/** Semantics tags for the tasks screen, consumed by `:client:feature:tasks:robots`. */
object TasksScreenTestTags {
    const val LIST = "tasks_list"
    const val EMPTY = "tasks_empty"
    const val ADD = "tasks_add"
    const val REFRESH = "tasks_refresh"
    const val RETRY = "tasks_retry"
    const val ERROR = "tasks_error"
    const val DETAIL = "tasks_detail"
    const val DETAIL_PLACEHOLDER = "tasks_detail_placeholder"
    const val DETAIL_TITLE = "tasks_detail_title"
    const val DETAIL_DONE = "tasks_detail_done"
    const val DETAIL_DELETE = "tasks_detail_delete"
    const val DETAIL_BACK = "tasks_detail_back"
    const val CREATE_TITLE = "tasks_create_title"
    const val CREATE_SUBMIT = "tasks_create_submit"
    const val CREATE_CANCEL = "tasks_create_cancel"

    fun row(title: String): String = "tasks_row_$title"

    fun rowDone(title: String): String = "tasks_row_done_$title"
}
