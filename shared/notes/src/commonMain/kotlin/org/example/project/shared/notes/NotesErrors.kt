package org.example.project.shared.notes

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.shared.common.ApiError

/**
 * The notes domain's per-operation Declared-error lens (ADR-0011): [NotesCreateError] is the exact
 * set `notes.create` commits to returning, handled exhaustively by the client for that call. The
 * status lives on the variant; code and status are frozen by `NotesDeclaredErrorFreezeTest`.
 *
 * [NotesQuotaExceeded] is a genuine domain rule (not a cross-cutting variant) — the per-account
 * character budget — so it carries its own `notes.*` code and rides the wire inside
 * `ErrorEnvelope`, exercising the per-domain error path end-to-end for a second domain. It is a
 * conflict with the account's current quota state.
 */
@Serializable sealed interface NotesCreateError : ApiError

@Serializable
@SerialName("notes.quota_exceeded")
data class NotesQuotaExceeded(
    @SerialName("quota") val quota: Int,
    @SerialName("used") val used: Int,
) : NotesCreateError {
    override val status: HttpStatusCode
        get() = HttpStatusCode.Conflict
}
