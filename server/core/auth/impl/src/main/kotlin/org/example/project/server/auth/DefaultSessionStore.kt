package org.example.project.server.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import org.example.project.server.auth.data.Sessions
import org.example.project.server.database.dbTransaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSessionStore(private val cache: SessionCache) : SessionStore {
    private val secureRandom = SecureRandom()

    override suspend fun issue(accountId: AccountId): Session {
        val token = generateToken()
        val now = Clock.System.now()
        val expiresAt = now + SESSION_TTL
        dbTransaction {
            Sessions.insert {
                it[Sessions.token] = token
                it[Sessions.accountId] = accountId.value
                it[createdAt] = now
                it[Sessions.expiresAt] = expiresAt
            }
        }
        return Session(token = token, accountId = accountId, expiresAt = expiresAt)
    }

    override suspend fun resolve(token: String): Principal? = cache.resolve(token, ::loadPrincipal)

    private suspend fun loadPrincipal(token: String): Principal? = dbTransaction {
        Sessions.selectAll()
            .where { Sessions.token eq token }
            .singleOrNull()
            ?.let { row ->
                if (row[Sessions.expiresAt] > Clock.System.now()) {
                    Principal(AccountId(row[Sessions.accountId]))
                } else {
                    null
                }
            }
    }

    override suspend fun revoke(token: String) {
        dbTransaction { Sessions.deleteWhere { Sessions.token eq token } }
        cache.invalidate(token)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        val SESSION_TTL = 30.days
        const val TOKEN_BYTES = 32
    }
}
