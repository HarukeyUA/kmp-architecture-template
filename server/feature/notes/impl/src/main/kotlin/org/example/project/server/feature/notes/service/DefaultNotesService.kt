package org.example.project.server.feature.notes.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import org.example.project.server.auth.Principal
import org.example.project.server.feature.auth.AuthService
import org.example.project.server.feature.notes.AuthoredNote
import org.example.project.server.feature.notes.NotesService
import org.example.project.server.feature.notes.data.NoteRepository
import org.example.project.server.web.Failure
import org.example.project.server.web.ambient
import org.example.project.server.web.declared
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.Validation
import org.example.project.shared.notes.NoteText
import org.example.project.shared.notes.NotesCreateError

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

    override suspend fun list(principal: Principal): Either<Failure<Nothing>, List<AuthoredNote>> =
        either {
            val authorEmail = authorEmail(principal).bind()
            repo.listFor(principal.accountId).map { AuthoredNote(it, authorEmail) }
        }

    override suspend fun create(
        principal: Principal,
        text: String,
    ): Either<Failure<NotesCreateError>, AuthoredNote> = either {
        // Shared shape check first (Ambient Validation); the stateful quota check is server-only
        // (ADR-0004) and Declared.
        val noteText = NoteText.of(text).getOrElse { ambient(Validation(listOf(it))) }
        val authorEmail = authorEmail(principal).bind()
        val note =
            repo
                .createWithinQuota(principal.accountId, noteText.value, NotesService.QUOTA)
                .getOrElse { declared(it) }
        AuthoredNote(note, authorEmail)
    }

    override suspend fun delete(
        principal: Principal,
        noteId: String,
    ): Either<Failure<Nothing>, Unit> = either {
        // Missing and not-yours collapse to the same Ambient NotFound — no existence disclosure.
        if (!repo.delete(principal.accountId, noteId)) ambient(NotFound("note"))
    }

    /**
     * The cross-domain call (ADR-0006): asks the auth domain's public service who the Principal is.
     * A deleted account mid-request surfaces as Ambient `Unauthorized` and propagates via `bind()`
     * — `me` declares nothing, so its `Failure<Nothing>` fits any caller's channel.
     */
    private suspend fun authorEmail(principal: Principal): Either<Failure<Nothing>, String> =
        either {
            authService.me(principal).bind().email
        }
}
