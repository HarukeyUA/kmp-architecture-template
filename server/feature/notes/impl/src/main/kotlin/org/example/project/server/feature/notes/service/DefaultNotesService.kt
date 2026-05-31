package org.example.project.server.feature.notes.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.Principal
import org.example.project.server.database.dbTransaction
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.feature.notes.data.Note
import org.example.project.server.feature.notes.data.NoteRepository
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.Validation
import org.example.project.shared.notes.CreateNoteRequest
import org.example.project.shared.notes.NoteResponse
import org.example.project.shared.notes.NoteText
import org.example.project.shared.notes.NotesQuotaExceeded

/**
 * The notes domain service. It owns the transaction (repositories assume an ambient one, ADR-0006)
 * and reaches the auth domain only through [AuthService] — its **public** contract — to resolve the
 * author's email. The accounts table is another domain's `:impl`; the module-assert task forbids
 * importing it, so this cross-domain call is the *only* legal coupling.
 */
@Inject
@ContributesBinding(AppScope::class)
class DefaultNotesService(private val repo: NoteRepository, private val authService: AuthService) :
    NotesService {

    override suspend fun list(principal: Principal): Either<ApiError, List<NoteResponse>> = either {
        val authorEmail = authorEmail(principal).bind()
        val notes = dbTransaction { repo.listFor(principal.accountId) }
        notes.map { it.toResponse(authorEmail) }
    }

    override suspend fun create(
        principal: Principal,
        request: CreateNoteRequest,
    ): Either<ApiError, NoteResponse> = either {
        // Shared shape check first; the stateful quota check is server-only (ADR-0004).
        val text = NoteText.of(request.text).mapLeft { Validation(listOf(it)) }.bind()
        val authorEmail = authorEmail(principal).bind()
        // The service owns the transaction; the quota read and insert are atomic (ADR-0006).
        dbTransaction {
            val used = repo.byteTotal(principal.accountId)
            ensure(used + text.value.length <= NotesService.QUOTA) {
                NotesQuotaExceeded(quota = NotesService.QUOTA, used = used)
            }
            repo.insert(principal.accountId, text.value).toResponse(authorEmail)
        }
    }

    override suspend fun delete(principal: Principal, noteId: String): Either<ApiError, Unit> =
        either {
            // Missing and not-yours collapse to the same NotFound — no existence disclosure.
            val deleted = dbTransaction { repo.delete(principal.accountId, noteId) }
            ensure(deleted) { NotFound("note") }
        }

    /**
     * The cross-domain call (ADR-0006): asks the auth domain's public service who the Principal is.
     * A deleted account mid-request surfaces as `Unauthorized` and propagates via `bind()`.
     */
    private suspend fun authorEmail(principal: Principal): Either<ApiError, String> = either {
        authService.me(principal).bind().email
    }

    private fun Note.toResponse(authorEmail: String): NoteResponse =
        NoteResponse(id = id, text = text, authorEmail = authorEmail, createdAt = createdAt)
}
