package org.example.project.core.ui.error

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import org.example.project.core.error.CallFailure
import org.example.project.core.ui.error.ErrorRenderer.ResourceResult

/**
 * Renders the [CallFailure] arms that carry a cross-cutting wire error (ADR-0011).
 * [CallFailure.Ambient] — and, as a fallback, an unhandled [CallFailure.Declared] — maps through
 * [toResourceResult]; per-domain Declared errors return `null` here (the feature that owns them
 * renders them inline) and fall through to the generic message. [CallFailure.Transport] is skipped:
 * it is a `DelegatingError` whose [NetworkError][org.example.project.core.error.NetworkError] is
 * handled by [NetworkErrorRenderer] via `CompositeErrorRenderer`'s delegation.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class CallFailureRenderer : ErrorRenderer<CallFailure<*>> {
    override fun resolveResource(error: CallFailure<*>): ResourceResult? =
        when (error) {
            is CallFailure.Declared -> error.error.toResourceResult()
            is CallFailure.Ambient -> error.error.toResourceResult()
            is CallFailure.Transport -> null
        }
}
