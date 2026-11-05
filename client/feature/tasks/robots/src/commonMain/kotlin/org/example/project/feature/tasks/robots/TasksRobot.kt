package org.example.project.feature.tasks.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.tasks.TasksScreenTestTags

/** Drives the tasks screen through its semantics tags. */
class TasksRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    fun awaitShown() {
        awaitTag("tasks list", TasksScreenTestTags.LIST)
    }

    fun awaitEmpty() {
        awaitTag("empty tasks list", TasksScreenTestTags.EMPTY)
    }

    /** Creates a task through the form and waits for its row, which means the server took it. */
    fun createTask(title: String) {
        awaitEnabled("add task button", TasksScreenTestTags.ADD)
        clickTag(TasksScreenTestTags.ADD)
        awaitTag("create task form", TasksScreenTestTags.CREATE_TITLE)
        nodes.onNodeWithTag(TasksScreenTestTags.CREATE_TITLE).performTextInput(title)
        clickTag(TasksScreenTestTags.CREATE_SUBMIT)
        awaitTask(title)
    }

    fun awaitTask(title: String) {
        awaitTag("task row \"$title\"", TasksScreenTestTags.row(title))
    }

    fun awaitTaskGone(title: String) {
        await("no task row \"$title\"") { !tagExists(TasksScreenTestTags.row(title)) }
    }

    fun openTask(title: String) {
        clickTag(TasksScreenTestTags.row(title))
        awaitTag("task details", TasksScreenTestTags.DETAIL_TITLE)
    }

    fun toggleDone(title: String) {
        clickTag(TasksScreenTestTags.rowDone(title))
    }

    fun awaitDone(title: String, done: Boolean) {
        val tag = TasksScreenTestTags.rowDone(title)
        await("task \"$title\" done=$done") {
            val matcher = hasTestTag(tag) and if (done) isOn() else isOff()
            nodes.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Deletes the open task. On a narrow window this also returns to the list. */
    fun deleteOpenTask() {
        clickTag(TasksScreenTestTags.DETAIL_DELETE)
    }
}
