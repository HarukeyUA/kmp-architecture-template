package org.example.project.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import org.example.project.core.error.CallFailure
import org.example.project.feature.session.ClientSession
import org.example.project.feature.session.SessionState
import org.example.project.server.testing.serverTest
import org.example.project.shared.auth.AccountResponse
import org.example.project.shared.auth.AuthResource
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.InvalidCredentials

/**
 * The client data layer against the real server, proving what the feature's MockEngine tests can't:
 * both ends of the seam agree on routes, tokens and typed errors.
 */
class AuthRepositoryIntegrationTest {

    @Test
    fun `signup, logout, then login again through the repository`() = serverTest {
        val client = clientStack()

        assertThat(client.auth.signup(EMAIL, PASSWORD).isRight()).isEqualTo(true)
        assertThat(client.sessions.current).isInstanceOf(SessionState.Bound::class)

        client.auth.logout()
        assertThat(client.sessions.current).isEqualTo(SessionState.Unbound)

        assertThat(client.auth.login(EMAIL, PASSWORD).isRight()).isEqualTo(true)
        assertThat(client.sessions.current).isInstanceOf(SessionState.Bound::class)
    }

    @Test
    fun `the server's Declared errors arrive typed and leave a signed-out device signed out`() =
        serverTest {
            clientStack().auth.signup(EMAIL, PASSWORD)
            val signedOut = clientStack()

            assertThat(signedOut.auth.signup(EMAIL, PASSWORD).leftOrNull())
                .isEqualTo(CallFailure.Declared(EmailTaken))
            assertThat(signedOut.auth.login(EMAIL, "wrongpassword99").leftOrNull())
                .isEqualTo(CallFailure.Declared(InvalidCredentials))
            assertThat(signedOut.sessions.current).isEqualTo(SessionState.Unbound)
        }

    @Test
    fun `a rejected access token refreshes against the real server and the call retries`() =
        serverTest {
            val client = clientStack()
            client.auth.signup(EMAIL, PASSWORD)
            val refreshToken = client.boundSession().refreshToken
            client.sessions.setBound(ClientSession(REJECTED_ACCESS_TOKEN, refreshToken))

            val me = client.http.get(AuthResource.Me())

            assertThat(me.status).isEqualTo(HttpStatusCode.OK)
            assertThat(me.body<AccountResponse>().email).isEqualTo(EMAIL)
            assertThat(client.boundSession().refreshToken).isEqualTo(refreshToken)
        }

    @Test
    fun `a revoked refresh token signs the device out`() = serverTest {
        val client = clientStack()
        client.auth.signup(EMAIL, PASSWORD)
        val revoked = client.boundSession()
        client.auth.logout()
        client.sessions.setBound(ClientSession(REJECTED_ACCESS_TOKEN, revoked.refreshToken))

        val me = client.http.get(AuthResource.Me())

        assertThat(me.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(client.sessions.current).isEqualTo(SessionState.Unbound)
    }

    private fun ClientStack.boundSession(): ClientSession =
        (sessions.current as SessionState.Bound).session

    private companion object {
        const val EMAIL = "carol@example.com"
        const val PASSWORD = "hunter2hunter2"
        const val REJECTED_ACCESS_TOKEN = "not-a-jwt"
    }
}
