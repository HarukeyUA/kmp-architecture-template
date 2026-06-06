package org.example.project.server.auth

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
    private val base64Url = Base64.getUrlEncoder().withoutPadding()

    override suspend fun issue(accountId: AccountId): Session {
        val token = generateToken()
        val tokenHash = hashToken(token)
        val now = Clock.System.now()
        val expiresAt = now + SESSION_TTL
        dbTransaction {
            Sessions.insert {
                it[Sessions.tokenHash] = tokenHash
                it[Sessions.accountId] = accountId.value
                it[createdAt] = now
                it[Sessions.expiresAt] = expiresAt
            }
        }
        return Session(token = token, accountId = accountId, expiresAt = expiresAt)
    }

    override suspend fun resolve(token: String): Principal? =
        cache.resolve(hashToken(token), ::loadPrincipal)

    private suspend fun loadPrincipal(tokenHash: String): CachedSession? = dbTransaction {
        Sessions.selectAll()
            .where { Sessions.tokenHash eq tokenHash }
            .singleOrNull()
            ?.let { row ->
                val expiresAt = row[Sessions.expiresAt]
                if (expiresAt > Clock.System.now()) {
                    CachedSession(
                        principal = Principal(AccountId(row[Sessions.accountId])),
                        expiresAt = expiresAt,
                    )
                } else {
                    null
                }
            }
    }

    override suspend fun revoke(token: String) {
        val tokenHash = hashToken(token)
        dbTransaction { Sessions.deleteWhere { Sessions.tokenHash eq tokenHash } }
        cache.invalidate(tokenHash)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return base64Url.encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val bytes =
            MessageDigest.getInstance(TOKEN_HASH_ALGORITHM)
                .digest(token.toByteArray(StandardCharsets.UTF_8))
        return base64Url.encodeToString(bytes)
    }

    private companion object {
        val SESSION_TTL = 30.days
        const val TOKEN_BYTES = 32
        const val TOKEN_HASH_ALGORITHM = "SHA-256"
    }
}
