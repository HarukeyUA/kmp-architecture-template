package org.example.project.server.feature.notes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.time.Clock
import org.example.project.server.auth.AccountId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Reads/writes notes, returning the domain [Note] (never a `ResultRow`) so the service is
 * unit-testable against a fake. Every query is scoped to the owning [AccountId] — that scoping *is*
 * the authorization boundary. Assumes an **ambient transaction** opened by the service — never
 * opens its own (ADR-0006).
 */
interface NoteRepository {
    suspend fun listFor(accountId: AccountId): List<Note>

    /** Total characters across the account's notes — the input to the per-account quota check. */
    suspend fun byteTotal(accountId: AccountId): Int

    suspend fun insert(accountId: AccountId, text: String): Note

    /** Deletes the note iff it exists *and* belongs to [accountId]; returns whether a row went. */
    suspend fun delete(accountId: AccountId, noteId: String): Boolean
}

@Inject
@ContributesBinding(AppScope::class)
class DefaultNoteRepository : NoteRepository {
    override suspend fun listFor(accountId: AccountId): List<Note> =
        Notes.selectAll()
            .where { Notes.accountId eq accountId.value }
            .orderBy(Notes.createdAt to SortOrder.DESC)
            .map { it.toNote() }

    override suspend fun byteTotal(accountId: AccountId): Int =
        Notes.selectAll()
            .where { Notes.accountId eq accountId.value }
            .sumOf { it[Notes.text].length }

    override suspend fun insert(accountId: AccountId, text: String): Note {
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

    override suspend fun delete(accountId: AccountId, noteId: String): Boolean {
        val uuid = runCatching { UUID.fromString(noteId) }.getOrNull() ?: return false
        return Notes.deleteWhere { (Notes.id eq uuid) and (Notes.accountId eq accountId.value) } > 0
    }

    private fun ResultRow.toNote(): Note =
        Note(
            id = this[Notes.id].toString(),
            accountId = AccountId(this[Notes.accountId]),
            text = this[Notes.text],
            createdAt = this[Notes.createdAt],
        )
}
