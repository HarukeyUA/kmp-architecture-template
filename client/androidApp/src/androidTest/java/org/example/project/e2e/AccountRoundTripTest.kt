package org.example.project.e2e

import org.example.project.e2e.journeys.AccountJourney
import org.junit.Test

/** Signs up, logs out and logs back in through the real UI against the real dev server. */
class AccountRoundTripTest : E2eTest() {
    private val account by journey<AccountJourney>()

    @Test
    fun signUpLogOutAndLogInRoundTripTheServer() {
        val email = freshEmail()

        account.signUp(email, PASSWORD)
        account.logOut()
        account.logIn(email, PASSWORD)
    }
}
