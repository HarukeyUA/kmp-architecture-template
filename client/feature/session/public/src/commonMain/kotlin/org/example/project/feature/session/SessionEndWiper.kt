package org.example.project.feature.session

/** Keeps one account's local data from reaching the next account signed in on this device. */
interface SessionEndWiper {
    /**
     * Empties the local database whenever a sign-in ends, however it ended, and also on a launch
     * with no session, which covers a process killed before its wipe ran. Never returns.
     */
    suspend fun watch()
}
