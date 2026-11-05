package org.example.project.feature.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The session file's contents. Renaming a serial name leaves existing files unreadable. */
@Serializable
internal sealed interface StoredSession {

    @Serializable @SerialName("absent") data object Absent : StoredSession

    @Serializable
    @SerialName("present")
    data class Present(val session: ClientSession) : StoredSession

    /** What a file that does not decode reads as; never written. A write replaces the file. */
    @Serializable @SerialName("unreadable") data object Unreadable : StoredSession
}
