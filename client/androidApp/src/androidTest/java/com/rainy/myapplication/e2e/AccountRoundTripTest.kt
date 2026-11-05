package com.rainy.myapplication.e2e

import kotlin.random.Random
import org.junit.Test

/** Signs up, logs out and logs back in through the real UI against the real dev server. */
class AccountRoundTripTest : E2eTest() {

    @Test
    fun signupLogoutAndLoginRoundTripTheServer() {
        // The Orchestrator clears app data between tests, but the dev server's database persists.
        val email = "e2e-${Random.nextLong(Long.MAX_VALUE)}@example.com"
        signUpAndAwaitHome(email, PASSWORD)

        home.logOut()

        login.awaitShown()
        login.typeEmail(email)
        login.typePassword(PASSWORD)
        login.logIn()

        home.awaitShown()
    }

    private companion object {
        const val PASSWORD = "correct-horse-battery-staple"
    }
}
