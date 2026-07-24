package com.rainy.myapplication.e2e

import org.junit.Test

/**
 * The template's example E2E flow: signs up through the real UI and lands on the signed-in main
 * screen — splash routing, the login form, session persistence, and root navigation, driven
 * entirely through semantics tags. Extend by replacing the fake `AuthRepositoryImpl` with your
 * backend and asserting on real round trips.
 */
class LoginFlowTest : E2eTest() {

    @Test
    fun signupLandsOnMain() {
        signUpAndAwaitMain(email = "e2e@example.com", password = "correct-horse-battery-staple")
    }
}
