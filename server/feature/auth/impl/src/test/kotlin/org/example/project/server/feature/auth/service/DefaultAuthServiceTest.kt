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
import org.example.project.server.auth.AccountId
import org.example.project.server.auth.Principal
import org.example.project.server.auth.Session
import org.example.project.server.auth.SessionStore
import org.example.project.server.feature.auth.Account
import org.example.project.server.feature.auth.PasswordHasher
import org.example.project.server.feature.auth.data.AccountRepository
import org.example.project.server.feature.auth.data.Credential
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Unauthorized

/**
 * Pins the login failure-path contract against fakes — above all the **timing-equalization**
 * shape, which no integration test can assert: an unknown email must burn exactly one dummy
 * verify (so it costs the same as a wrong password), while a malformed email stays fast on
 * purpose. If a refactor drops the dummy verify as "dead work", these tests are what fail.
 */
class DefaultAuthServiceTest {

    private val accountId = AccountId(UUID.randomUUID())
    private val repo =
        FakeAccountRepository(
            mapOf("known@example.com" to Credential(accountId, "hash:correct horse"))
        )
    private val hasher = RecordingHasher()
    private val service = DefaultAuthService(repo, hasher, FakeSessionStore())

    @Test
    fun `unknown email burns exactly one dummy verify and collapses to Unauthorized`() = runTest {
        val result = service.login("unknown@example.com", "whatever password")

        assertThat(result).isEqualTo(Unauthorized.left())
        assertThat(hasher.dummyVerifyCalls).isEqualTo(1)
        assertThat(hasher.verifyCalls).isEqualTo(0)
    }

    @Test
    fun `wrong password performs one real verify and no dummy`() = runTest {
        val result = service.login("known@example.com", "wrong password")

        assertThat(result).isEqualTo(Unauthorized.left())
        assertThat(hasher.verifyCalls).isEqualTo(1)
        assertThat(hasher.dummyVerifyCalls).isEqualTo(0)
    }

    @Test
    fun `malformed email fails fast with no hasher work at all`() = runTest {
        val result = service.login("not an email", "whatever password")

        assertThat(result).isEqualTo(Unauthorized.left())
        assertThat(hasher.verifyCalls).isEqualTo(0)
        assertThat(hasher.dummyVerifyCalls).isEqualTo(0)
    }

    @Test
    fun `valid credentials issue a session`() = runTest {
        val result = service.login("known@example.com", "correct horse")

        assertThat(result.map { it.accountId }).isEqualTo(accountId.right())
    }
}

private class FakeAccountRepository(
    private val credentials: Map<String, Credential>,
) : AccountRepository {
    override suspend fun findById(id: AccountId): Account? = null

    override suspend fun findCredentialByEmail(email: String): Credential? = credentials[email]

    override suspend fun create(email: String, passwordHash: String): Either<ApiError, Account> =
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

private class FakeSessionStore : SessionStore {
    override suspend fun issue(accountId: AccountId): Session =
        Session(token = "opaque-token", accountId = accountId, expiresAt = Instant.DISTANT_FUTURE)

    override suspend fun resolve(token: String): Principal? = null

    override suspend fun revoke(token: String) = Unit

    override suspend fun revokeAllFor(accountId: AccountId) = Unit
}
