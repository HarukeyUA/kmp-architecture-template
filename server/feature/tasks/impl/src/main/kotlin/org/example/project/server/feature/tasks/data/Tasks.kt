package org.example.project.server.feature.tasks.data

import org.example.project.server.database.utcTimestamp
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

/** `account_id` is not a foreign key: `accounts` belongs to another domain's `:impl`. */
internal object Tasks : Table("tasks") {
    val id = javaUUID("id")
    val accountId = javaUUID("account_id")
    val title = text("title")
    val done = bool("done")
    val createdAt = utcTimestamp("created_at")
    override val primaryKey = PrimaryKey(id)

    init {
        index("tasks_account_idx", false, accountId)
    }
}
