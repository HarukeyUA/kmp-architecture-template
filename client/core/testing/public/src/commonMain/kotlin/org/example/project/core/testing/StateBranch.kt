package org.example.project.core.testing

/**
 * Narrows a state (or nested branch holder) to branch [B], throwing a clear AssertionError
 * otherwise.
 */
inline fun <reified B : Any> Any?.asBranch(): B =
    this as? B
        ?: throw AssertionError(
            "Expected " +
                B::class.simpleName +
                " but was " +
                (this?.let { "${it::class.simpleName}: $it" } ?: "null")
        )
