package org.example.project.shared.tasks

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class CreateTaskRequest(@SerialName("title") val title: String)

@Serializable data class SetTaskDoneRequest(@SerialName("done") val done: Boolean)

@Serializable
data class TaskResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("done") val done: Boolean,
    @SerialName("createdAt") val createdAt: Instant,
)

/** A wrapper rather than a bare array, so paging can be added without breaking the wire shape. */
@Serializable data class TaskListResponse(@SerialName("tasks") val tasks: List<TaskResponse>)
