package org.example.project.core.database.testing

import org.example.project.core.database.DatabaseWiper

class FakeDatabaseWiper : DatabaseWiper {
    var wipeCalls: Int = 0
        private set

    override suspend fun wipe() {
        wipeCalls++
    }
}
