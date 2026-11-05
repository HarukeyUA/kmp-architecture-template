package org.example.project.feature.session

/** What the session contract answers when asked for this device's session. */
sealed interface SessionState {

    /** No account is signed in on this device. */
    data object Unbound : SessionState

    data class Bound(val session: ClientSession) : SessionState
}
