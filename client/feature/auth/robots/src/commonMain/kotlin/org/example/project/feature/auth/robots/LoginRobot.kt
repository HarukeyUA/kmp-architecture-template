package org.example.project.feature.auth.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import org.example.project.core.robots.E2eScope
import org.example.project.core.robots.Robot
import org.example.project.core.robots.Wait
import org.example.project.feature.auth.LoginScreenTestTags

/** Drives the login screen through its semantics tags. */
@Inject
@ClassKey
@ContributesIntoMap(E2eScope::class)
class LoginRobot(nodes: SemanticsNodeInteractionsProvider, wait: Wait) : Robot(nodes, wait) {
    /** Waits for the login screen — the app's landing state on a device with no session. */
    fun awaitShown() {
        awaitTag("login screen", LoginScreenTestTags.EMAIL)
    }

    fun typeEmail(email: String) {
        nodes.onNodeWithTag(LoginScreenTestTags.EMAIL).performTextInput(email)
    }

    fun typePassword(password: String) {
        nodes.onNodeWithTag(LoginScreenTestTags.PASSWORD).performTextInput(password)
    }

    /**
     * Taps "Log in". On success the session is stored and navigation leaves this feature, so the
     * cross-screen wait for the signed-in screen belongs to the calling flow.
     */
    fun logIn() {
        clickTag(LoginScreenTestTags.LOG_IN)
    }

    /** Taps "Create account" — signup shares the login form's email/password fields. */
    fun createAccount() {
        clickTag(LoginScreenTestTags.CREATE_ACCOUNT)
    }
}
