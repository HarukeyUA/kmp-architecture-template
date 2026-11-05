package org.example.project.e2e

import org.example.project.e2e.journeys.ShellJourney
import org.example.project.feature.home.robots.HomeRobot
import org.junit.Test

/** The shell comes up offline and survives recreation. Runs against nothing but the device. */
class ShellSmokeTest : E2eTest() {
    private val shell by journey<ShellJourney>()
    private val home by robot<HomeRobot>()

    @Test
    fun launchReachesHomeAndSurvivesRecreation() {
        shell.launchToHome()

        host.recreateActivity()

        home.awaitShown()
    }
}
