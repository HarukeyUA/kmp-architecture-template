package org.example.project.feature.tasks

import kotlin.time.Instant

data class Task(val id: String, val title: String, val done: Boolean, val createdAt: Instant)
