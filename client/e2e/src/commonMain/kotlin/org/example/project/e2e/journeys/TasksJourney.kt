package org.example.project.e2e.journeys

import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import org.example.project.core.robots.E2eScope
import org.example.project.e2e.Journey
import org.example.project.feature.main.robots.MainRobot
import org.example.project.feature.tasks.robots.TasksRobot

/** What a user does to a task, starting from any signed-in tab. Each step ends on the list. */
@Inject
@ClassKey
@ContributesIntoMap(E2eScope::class)
class TasksJourney(private val main: MainRobot, private val tasks: TasksRobot) : Journey {

    fun openList() {
        main.openTasksTab()
        tasks.awaitShown()
    }

    fun create(title: String) {
        openList()
        tasks.createTask(title)
    }

    fun toggleDone(title: String, done: Boolean) {
        openList()
        tasks.toggleDone(title)
        tasks.awaitDone(title, done)
    }

    // The list comes back before the row is checked: on a phone the row is also absent while the
    // detail pane covers the list, which would pass for a finished delete.
    fun delete(title: String) {
        openList()
        tasks.openTask(title)
        tasks.deleteOpenTask()
        tasks.awaitShown()
        tasks.awaitTaskGone(title)
    }
}
