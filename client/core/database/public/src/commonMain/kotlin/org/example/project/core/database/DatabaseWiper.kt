package org.example.project.core.database

interface DatabaseWiper {
    /** Deletes every row of every table and leaves the database open for the next account. */
    suspend fun wipe()
}
