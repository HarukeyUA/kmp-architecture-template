package org.example.project.server.feature.auth.data

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.time.Clock
import org.example.project.server.auth.AccountId
import org.example.project.server.database.dbTransaction
import org.example.project.server.feature.auth.Account
import org.example.project.shared.auth.EmailTaken
import org.example.project.shared.common.ApiError
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Reads/writes accounts, returning the domain [Account] (never a `ResultRow`) so the service is
 * unit-testable against a fake. The Argon2id hash is queried only via [findCredentialByEmail] — the
 * login path's verify step — and never rides along on account lookups. Repository methods are
 * transaction-safe: they open a transaction when called alone and join the caller's transaction
 * when one exists (ADR-0006).
 */
interface AccountRepository {
    suspend fun findById(id: AccountId): Account?

    suspend fun findCredentialByEmail(email: String): Credential?

    suspend fun create(email: String, passwordHash: String): Either<ApiError, Account>
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultAccountRepository : AccountRepository {
    override suspend fun findById(id: AccountId): Account? = dbTransaction {
        Accounts.selectAll().where { Accounts.id eq id.value }.singleOrNull()?.toAccount()
    }

    override suspend fun findCredentialByEmail(email: String): Credential? = dbTransaction {
        Accounts.selectAll()
            .where { Accounts.email eq email }
            .singleOrNull()
            ?.let { Credential(AccountId(it[Accounts.id]), it[Accounts.passwordHash]) }
    }

    override suspend fun create(email: String, passwordHash: String): Either<ApiError, Account> =
        either {
            catch({
                dbTransaction {
                    val id = UUID.randomUUID()
                    Accounts.insert {
                        it[Accounts.id] = id
                        it[Accounts.email] = email
                        it[Accounts.passwordHash] = passwordHash
                        it[createdAt] = Clock.System.now()
                    }
                    Account(id = AccountId(id), email = email)
                }
            }) { e: ExposedSQLException ->
                ensure(e.sqlState != UNIQUE_VIOLATION) { EmailTaken }
                throw e
            }
        }

    private fun ResultRow.toAccount(): Account =
        Account(id = AccountId(this[Accounts.id]), email = this[Accounts.email])

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
