package org.example.project.server.feature.auth.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.UUID
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.example.project.server.auth.AccessToken
import org.example.project.server.auth.AccessTokenIssuer
import org.example.project.server.auth.AccountId
import org.example.project.server.auth.Principal
import org.example.project.server.auth.Session
import org.example.project.server.auth.SessionStore
import org.example.project.server.feature.auth.Account
import org.example.project.server.feature.auth.PasswordHasher
import org.example.project.server.feature.auth.data.AccountRepository
import org.example.project.server.feature.auth.data.Credential
import org.example.project.server.web.Failure
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.auth.InvalidCredentials
import org.example.project.shared.auth.SessionExpired

/**
 * Pins the login failure-path contract against fakes — above all the **timing-equalization** shape,
 * which no integration test can assert: an unknown email must burn exactly one dummy verify (so it
 * costs the same as a wrong password), while a malformed email stays fast on purpose. If a refactor
 * drops the dummy verify as "dead work", these tests are what fail.
 */
class DefaultAuthServiceTest {

    private val accountId = AccountId(UUID.randomUUID())
    private val repo =
        FakeAccountRepository(
            mapOf("known@example.com" to Credential(accountId, "hash:correct horse"))
        )
    private val hasher = RecordingHasher()
    private val sessionStore = FakeSessionStore()
    private val service = DefaultAuthService(repo, hasher, sessionStore, FakeAccessTokenIssuer())

    @Test
    fun `unknown email burns exactly one dummy verify and collapses to InvalidCredentials`() =
        runTest {
            val result = service.login("unknown@example.com", "whatever password")

            assertThat(result).isEqualTo(Failure.Declared(InvalidCredentials).left())
            assertThat(hasher.dummyVerifyCalls).isEqualTo(1)
            assertThat(hasher.verifyCalls).isEqualTo(0)
        }

    @Test
    fun `wrong password performs one real verify and no dummy`() = runTest {
        val result = service.login("known@example.com", "wrong password")

        assertThat(result).isEqualTo(Failure.Declared(InvalidCredentials).left())
        assertThat(hasher.verifyCalls).isEqualTo(1)
        assertThat(hasher.dummyVerifyCalls).isEqualTo(0)
    }

    @Test
    fun `malformed email fails fast with no hasher work at all`() = runTest {
        val result = service.login("not an email", "whatever password")

        assertThat(result).isEqualTo(Failure.Declared(InvalidCredentials).left())
        assertThat(hasher.verifyCalls).isEqualTo(0)
        assertThat(hasher.dummyVerifyCalls).isEqualTo(0)
    }

    @Test
    fun `valid credentials issue an access token and a session`() = runTest {
        val result = service.login("known@example.com", "correct horse")

        assertThat(result.map { it.session.accountId }).isEqualTo(accountId.right())
        assertThat(result.map { it.accessToken.token })
            .isEqualTo("access:${accountId.value}".right())
    }

    @Test
    fun `refresh with a live session mints an access token without touching the hasher`() =
        runTest {
            val tokens = service.login("known@example.com", "correct horse").getOrNull()!!
            hasher.verifyCalls = 0

            val result = service.refresh(tokens.session.token)

            assertThat(result.map { it.token }).isEqualTo("access:${accountId.value}".right())
            assertThat(hasher.verifyCalls).isEqualTo(0)
            assertThat(hasher.dummyVerifyCalls).isEqualTo(0)
        }

    @Test
    fun `refresh with an unknown or revoked token collapses to SessionExpired`() = runTest {
        assertThat(service.refresh("never-issued"))
            .isEqualTo(Failure.Declared(SessionExpired).left())

        val tokens = service.login("known@example.com", "correct horse").getOrNull()!!
        service.logout(tokens.session.token)
        assertThat(service.refresh(tokens.session.token))
            .isEqualTo(Failure.Declared(SessionExpired).left())
    }
}

private class FakeAccountRepository(private val credentials: Map<String, Credential>) :
    AccountRepository {
    override suspend fun findById(id: AccountId): Account? = null

    override suspend fun findCredentialByEmail(email: String): Credential? = credentials[email]

    override suspend fun create(email: String, passwordHash: String): Either<EmailTaken, Account> =
        Account(AccountId(UUID.randomUUID()), email).right()
}

/** Verifies by comparing against the `"hash:" + password` convention; counts every call. */
private class RecordingHasher : PasswordHasher {
    var verifyCalls = 0
    var dummyVerifyCalls = 0

    override suspend fun hash(password: String): String = "hash:$password"

    override suspend fun verify(password: String, hash: String): Boolean {
        verifyCalls++
        return hash == "hash:$password"
    }

    override suspend fun verifyDummy(password: String) {
        dummyVerifyCalls++
    }
}

/** Issues predictable per-account tokens so tests can assert which account was granted access. */
private class FakeAccessTokenIssuer : AccessTokenIssuer {
    override fun issue(accountId: AccountId): AccessToken =
        AccessToken(token = "access:${accountId.value}", expiresAt = Instant.DISTANT_FUTURE)
}

/** In-memory issue/resolve/revoke, enough to pin the refresh contract against. */
private class FakeSessionStore : SessionStore {
    private val live = mutableMapOf<String, AccountId>()
    private var counter = 0

    override suspend fun issue(accountId: AccountId): Session {
        val token = "opaque-token-${counter++}"
        live[token] = accountId
        return Session(token = token, accountId = accountId, expiresAt = Instant.DISTANT_FUTURE)
    }

    override suspend fun resolve(token: String): Principal? = live[token]?.let(::Principal)

    override suspend fun revoke(token: String) {
        live.remove(token)
    }

    override suspend fun revokeAllFor(accountId: AccountId) {
        live.entries.removeAll { it.value == accountId }
    }
}
