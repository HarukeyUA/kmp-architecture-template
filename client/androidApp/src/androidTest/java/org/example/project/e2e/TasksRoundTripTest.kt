package org.example.project.e2e

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.example.project.e2e.journeys.AccountJourney
import org.example.project.e2e.journeys.TasksJourney
import org.example.project.feature.tasks.robots.TasksRobot
import org.junit.Test

/**
 * Task writes against the real dev server. Logging out wipes the local cache, so whatever the list
 * shows after logging back in came from the server, not from Room.
 */
class TasksRoundTripTest : E2eTest() {
    private val account by journey<AccountJourney>()
    private val tasksJourney by journey<TasksJourney>()
    private val tasks by robot<TasksRobot>()

    @Test
    fun createToggleAndDeleteReachTheServer() {
        val email = freshEmail()
        account.signUp(email, PASSWORD)

        tasksJourney.create(TITLE)
        tasksJourney.toggleDone(TITLE, done = true)
        reLogIn(email)
        tasks.awaitTask(TITLE)
        tasks.awaitDone(TITLE, done = true)

        tasksJourney.delete(TITLE)
        reLogIn(email)
        tasks.awaitEmpty()
    }

    @Test
    fun aSecondAccountNeverSeesTheFirstAccountsTasks() {
        val firstEmail = freshEmail()
        account.signUp(firstEmail, PASSWORD)
        tasksJourney.create(TITLE)
        await("task cached") { cachedTasks().isNotEmpty() }

        account.logOut()
        await("task cache wiped") { cachedTasks().isEmpty() }

        account.signUp(freshEmail(), PASSWORD)
        tasks.awaitEmpty()

        account.logOut()
        account.logIn(firstEmail, PASSWORD)
        tasks.awaitTask(TITLE)
    }

    @Test
    fun anOpenTaskSurvivesRecreationAndBackReturnsToTheList() {
        account.signUp(freshEmail(), PASSWORD)
        tasksJourney.create(TITLE)
        tasks.openTask(TITLE)

        host.recreateActivity()
        tasks.awaitDetailShown()

        host.pressBack()
        tasks.awaitShown()
        tasks.awaitTask(TITLE)
    }

    private fun reLogIn(email: String) {
        account.logOut()
        account.logIn(email, PASSWORD)
    }

    private fun cachedTasks() = runBlocking { e2eGraph.taskDao.observeAll().first() }

    private companion object {
        const val TITLE = "Water the plants"
    }
}
