package org.example.project.core.ui.error

import org.example.project.client.core.ui.Res
import org.example.project.client.core.ui.error_conflict
import org.example.project.client.core.ui.error_forbidden
import org.example.project.client.core.ui.error_not_found
import org.example.project.client.core.ui.error_rate_limited
import org.example.project.client.core.ui.error_unauthorized
import org.example.project.client.core.ui.error_validation
import org.example.project.core.ui.error.ErrorRenderer.ResourceResult
import org.example.project.shared.common.ApiError
import org.example.project.shared.common.Conflict
import org.example.project.shared.common.Forbidden
import org.example.project.shared.common.Internal
import org.example.project.shared.common.NotFound
import org.example.project.shared.common.RateLimited
import org.example.project.shared.common.Unauthorized
import org.example.project.shared.common.UnknownApiError
import org.example.project.shared.common.Validation

/**
 * Maps a cross-cutting [ApiError] (carried by
 * [NetworkError.Api][org.example.project.core.error.NetworkError.Api]) to a localized message.
 *
 * Per the interim stop-gap (ADR-0005) this matches the known variants and lets everything else —
 * [Internal], [UnknownApiError], and (for now) per-domain errors — return `null`, i.e. fall through
 * to the generic fallback in `CompositeErrorRenderer`. When the sealed per-domain grouping lands
 * (Phase 7), each domain narrows its own variants in its own renderer.
 */
fun ApiError.toResourceResult(): ResourceResult? =
    when (this) {
        Unauthorized -> ResourceResult(Res.string.error_unauthorized)
        Forbidden -> ResourceResult(Res.string.error_forbidden)
        is NotFound -> ResourceResult(Res.string.error_not_found)
        is Conflict -> ResourceResult(Res.string.error_conflict)
        is Validation -> ResourceResult(Res.string.error_validation)
        is RateLimited -> ResourceResult(Res.string.error_rate_limited)
        Internal,
        is UnknownApiError -> null
        else -> null
    }
