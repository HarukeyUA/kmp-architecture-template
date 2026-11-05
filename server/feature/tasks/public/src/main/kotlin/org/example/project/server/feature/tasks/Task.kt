package org.example.project.server.feature.tasks

import kotlin.time.Instant
import org.example.project.server.auth.AccountId

data class Task(
    val id: String,
    val accountId: AccountId,
    val title: String,
    val done: Boolean,
    val createdAt: Instant,
)
