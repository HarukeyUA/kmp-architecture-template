package org.example.project.server.feature.notes.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.Principal
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.notes.AuthoredNote
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.feature.notes.data.NoteRepository
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.Validation
import org.example.project.shared.notes.NoteText

/**
 * The notes domain service. It owns use-case orchestration while [NoteRepository] owns Exposed,
 * transactions, and persistence-backed invariants (ADR-0006). Cross-domain access still goes only
 * through [AuthService] — its **public**, domain-typed contract (ADR-0003 as amended) — to resolve
 * the author's email. The accounts table is another domain's `:impl`; the module-assert task
 * forbids importing it, so this cross-domain call is the *only* legal coupling.
 */
@Inject
@ContributesBinding(AppScope::class)
class DefaultNotesService(private val repo: NoteRepository, private val authService: AuthService) :
    NotesService {

    override suspend fun list(principal: Principal): Either<ApiError, List<AuthoredNote>> =
        either {
            val authorEmail = authorEmail(principal).bind()
            repo.listFor(principal.accountId).map { AuthoredNote(it, authorEmail) }
        }

    override suspend fun create(
        principal: Principal,
        text: String,
    ): Either<ApiError, AuthoredNote> = either {
        // Shared shape check first; the stateful quota check is server-only (ADR-0004).
        val noteText = NoteText.of(text).mapLeft { Validation(listOf(it)) }.bind()
        val authorEmail = authorEmail(principal).bind()
        val note =
            repo.createWithinQuota(principal.accountId, noteText.value, NotesService.QUOTA).bind()
        AuthoredNote(note, authorEmail)
    }

    override suspend fun delete(principal: Principal, noteId: String): Either<ApiError, Unit> =
        either {
            // Missing and not-yours collapse to the same NotFound — no existence disclosure.
            val deleted = repo.delete(principal.accountId, noteId)
            ensure(deleted) { NotFound("note") }
        }

    /**
     * The cross-domain call (ADR-0006): asks the auth domain's public service who the Principal is.
     * A deleted account mid-request surfaces as `Unauthorized` and propagates via `bind()`.
     */
    private suspend fun authorEmail(principal: Principal): Either<ApiError, String> = either {
        authService.me(principal).bind().email
    }
}
