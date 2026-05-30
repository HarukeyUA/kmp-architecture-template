package org.example.project.server.feature.auth.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.time.Clock
import org.example.project.server.auth.AccountId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Reads/writes accounts, returning the domain [Account] (never a `ResultRow`) so the service is
 * unit-testable against a fake. Assumes an **ambient transaction** opened by the service — never
 * opens its own (ADR-0006).
 */
interface AccountRepository {
    suspend fun findByEmail(email: String): Account?

    suspend fun findById(id: AccountId): Account?

    suspend fun insert(email: String, passwordHash: String): Account
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultAccountRepository : AccountRepository {
    override suspend fun findByEmail(email: String): Account? =
        Accounts.selectAll().where { Accounts.email eq email }.singleOrNull()?.toAccount()

    override suspend fun findById(id: AccountId): Account? =
        Accounts.selectAll().where { Accounts.id eq id.value }.singleOrNull()?.toAccount()

    override suspend fun insert(email: String, passwordHash: String): Account {
        val id = UUID.randomUUID()
        Accounts.insert {
            it[Accounts.id] = id
            it[Accounts.email] = email
            it[Accounts.passwordHash] = passwordHash
            it[createdAt] = Clock.System.now()
        }
        return Account(id = AccountId(id), email = email, passwordHash = passwordHash)
    }

    private fun ResultRow.toAccount(): Account =
        Account(
            id = AccountId(this[Accounts.id]),
            email = this[Accounts.email],
            passwordHash = this[Accounts.passwordHash],
        )
}
