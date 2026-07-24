package com.rainy.myapplication.e2e

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.rainy.myapplication.MainActivity
import org.example.project.core.robots.Wait
import org.example.project.feature.auth.robots.LoginRobot
import org.example.project.feature.main.robots.MainRobot
import org.junit.Rule
import org.junit.rules.RuleChain

/**
 * Base for the instrumented E2E flows: hosts [MainActivity] (running fully offline against the
 * local fake auth) and owns the rule chain, one robot per screen, and the cross-screen journeys the
 * flows share. Run via `./gradlew :client:androidApp:connectedDevDebugAndroidTest`. The
 * Orchestrator clears app data before each test, so every flow starts at the login screen.
 *
 * Cross-screen waits live here in the flow layer, never in the robots — robots would otherwise need
 * cross-feature deps mirroring the nav graph.
 */
abstract class E2eTest {

    protected val compose = createAndroidComposeRule<MainActivity>()

    // Compose rule outside the watcher: the compose host must still be alive when the failure
    // screenshot is captured.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(compose).around(FailureScreenshotRule(compose))

    protected val wait = Wait { description, timeoutMillis, condition ->
        compose.waitUntil(description, timeoutMillis, condition)
    }

    protected val login = LoginRobot(compose, wait)
    protected val main = MainRobot(compose, wait)

    /**
     * Shared flow preamble: signs up through the real UI (the splash screen routes a sessionless
     * launch to login; the fake accepts any credentials) and waits for the signed-in main screen.
     */
    protected fun signUpAndAwaitMain(email: String, password: String) {
        login.awaitShown()
        login.typeEmail(email)
        login.typePassword(password)
        login.createAccount()

        main.awaitShown()
    }
}
