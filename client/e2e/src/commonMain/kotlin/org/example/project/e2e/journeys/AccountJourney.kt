package org.example.project.e2e.journeys

import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import org.example.project.core.robots.E2eScope
import org.example.project.e2e.Journey
import org.example.project.feature.auth.robots.LoginRobot
import org.example.project.feature.main.robots.MainRobot
import org.example.project.feature.settings.robots.SettingsRobot

/** The trips across the session boundary: login screen to the signed-in tabs and back. */
@Inject
@ClassKey
@ContributesIntoMap(E2eScope::class)
class AccountJourney(
    private val login: LoginRobot,
    private val main: MainRobot,
    private val settings: SettingsRobot,
) : Journey {

    /** From the login screen, creates a fresh account. Ends on the signed-in tabs. */
    fun signUp(email: String, password: String) {
        fillCredentials(email, password)
        login.createAccount()
        main.awaitShown()
    }

    /** From the login screen, signs in to an existing account. Ends on the signed-in tabs. */
    fun logIn(email: String, password: String) {
        fillCredentials(email, password)
        login.logIn()
        main.awaitShown()
    }

    /** From the signed-in tabs, through Settings. Ends on the login screen. */
    fun logOut() {
        main.openSettingsTab()
        settings.awaitShown()
        settings.logOut()
        login.awaitShown()
    }

    private fun fillCredentials(email: String, password: String) {
        login.awaitShown()
        login.typeEmail(email)
        login.typePassword(password)
    }
}
