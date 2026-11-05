package org.example.project.feature.tasks.data

import kotlin.time.Instant
import org.example.project.core.database.entity.TaskEntity
import org.example.project.feature.tasks.Task
import org.example.project.shared.tasks.TaskResponse

private const val MICROS_PER_SECOND = 1_000_000L
private const val NANOS_PER_MICRO = 1_000

internal fun TaskResponse.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        done = done,
        createdAt =
            createdAt.epochSeconds * MICROS_PER_SECOND +
                createdAt.nanosecondsOfSecond / NANOS_PER_MICRO,
    )

internal fun TaskEntity.toTask(): Task =
    Task(
        id = id,
        title = title,
        done = done,
        createdAt =
            Instant.fromEpochSeconds(
                epochSeconds = createdAt.floorDiv(MICROS_PER_SECOND),
                nanosecondAdjustment = createdAt.mod(MICROS_PER_SECOND) * NANOS_PER_MICRO,
            ),
    )
