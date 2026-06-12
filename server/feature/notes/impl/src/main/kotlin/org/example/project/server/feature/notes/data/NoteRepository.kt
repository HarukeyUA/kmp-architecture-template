package org.example.project.server.feature.notes.data

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.time.Clock
import org.example.project.server.auth.AccountId
import org.example.project.server.database.advisoryXactLock
import org.example.project.server.database.dbTransaction
import org.example.project.server.feature.notes.Note
import org.example.project.shared.common.ApiError
import org.example.project.shared.notes.NotesQuotaExceeded
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.charLength
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Reads/writes notes, returning the domain [Note] (never a `ResultRow`) so the service is
 * unit-testable against a fake. Every query is scoped to the owning [AccountId] — that scoping *is*
 * the authorization boundary. Repository methods are transaction-safe: they open a transaction when
 * called alone and join the caller's transaction when one exists (ADR-0006).
 */
interface NoteRepository {
    suspend fun listFor(accountId: AccountId): List<Note>

    suspend fun createWithinQuota(
        accountId: AccountId,
        text: String,
        quota: Int,
    ): Either<ApiError, Note>

    /** Deletes the note iff it exists *and* belongs to [accountId]; returns whether a row went. */
    suspend fun delete(accountId: AccountId, noteId: String): Boolean
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultNoteRepository : NoteRepository {
    override suspend fun listFor(accountId: AccountId): List<Note> = dbTransaction {
        Notes.selectAll()
            .where { Notes.accountId eq accountId.value }
            .orderBy(Notes.createdAt to SortOrder.DESC)
            .map { it.toNote() }
    }

    override suspend fun createWithinQuota(
        accountId: AccountId,
        text: String,
        quota: Int,
    ): Either<ApiError, Note> = either {
        dbTransaction {
            advisoryXactLock(QUOTA_LOCK_NAMESPACE, accountId.value.hashCode())
            val used = usedCodePoints(accountId)
            val candidate = text.codePointCount(0, text.length)
            ensure(used + candidate <= quota) { NotesQuotaExceeded(quota = quota, used = used) }
            insert(accountId, text)
        }
    }

    override suspend fun delete(accountId: AccountId, noteId: String): Boolean = dbTransaction {
        val uuid = runCatching { UUID.fromString(noteId) }.getOrNull() ?: return@dbTransaction false
        Notes.deleteWhere { (Notes.id eq uuid) and (Notes.accountId eq accountId.value) } > 0
    }

    private fun usedCodePoints(accountId: AccountId): Int {
        // SUM(char_length(text)) on the DB: one scalar back, no text blobs loaded into the app.
        // Postgres char_length counts Unicode code points — the same unit codePointCount gives the
        // candidate above, so the gate, the error's `used`, and the stored truth never drift.
        val totalCodePoints = Notes.text.charLength().sum()
        return Notes.select(totalCodePoints)
            .where { Notes.accountId eq accountId.value }
            .single()[totalCodePoints] ?: 0
    }

    private fun insert(accountId: AccountId, text: String): Note {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        Notes.insert {
            it[Notes.id] = id
            it[Notes.accountId] = accountId.value
            it[Notes.text] = text
            it[createdAt] = now
        }
        return Note(id = id.toString(), accountId = accountId, text = text, createdAt = now)
    }

    private fun ResultRow.toNote(): Note =
        Note(
            id = this[Notes.id].toString(),
            accountId = AccountId(this[Notes.accountId]),
            text = this[Notes.text],
            createdAt = this[Notes.createdAt],
        )

    private companion object {
        /** Advisory-lock namespace for the per-account quota gate; distinct from other locks. */
        const val QUOTA_LOCK_NAMESPACE = 0x4E_0735 // "NOTES"
    }
}
