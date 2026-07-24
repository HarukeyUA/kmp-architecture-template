package org.example.project.core.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/**
 * Blocks until [condition] holds, failing the owning test with [description] after [timeoutMillis].
 * Robots synchronize through this instead of a concrete rule type so the same robot serves the
 * instrumented E2E suite and a future desktop `runComposeUiTest` suite; the instrumented flows
 * adapt `ComposeTestRule.waitUntil`.
 */
fun interface Wait {
    operator fun invoke(description: String, timeoutMillis: Long, condition: () -> Boolean)
}

/**
 * Base for per-feature screen robots: drives one screen through its semantics tags. Written against
 * [SemanticsNodeInteractionsProvider] plus the injected [Wait] primitive (rather than a concrete
 * rule type) so robots stay host-agnostic. Robots never wait for another feature's screen —
 * cross-screen waits compose in the flow test (see ARCHITECTURE.md § Module Dependency Rules).
 */
abstract class Robot(
    protected val nodes: SemanticsNodeInteractionsProvider,
    private val wait: Wait,
) {
    /** Waits up to [DEFAULT_TIMEOUT_MILLIS] for [condition]; [description] names the failure. */
    protected fun await(description: String, condition: () -> Boolean) {
        wait(description, DEFAULT_TIMEOUT_MILLIS, condition)
    }

    protected fun tagExists(tag: String): Boolean =
        nodes.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    protected fun awaitTag(description: String, tag: String) {
        await(description) { tagExists(tag) }
    }

    /** Waits until a node tagged [tag] is both present and enabled — for state-gated buttons. */
    protected fun awaitEnabled(description: String, tag: String) {
        await(description) {
            nodes.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun clickTag(tag: String) {
        nodes.onNodeWithTag(tag).performClick()
    }

    companion object {
        // Generous budget: loopback is fast, emulator cold JITs are not.
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
    }
}
