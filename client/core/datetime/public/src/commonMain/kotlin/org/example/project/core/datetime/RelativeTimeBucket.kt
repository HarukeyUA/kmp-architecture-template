package org.example.project.core.datetime

/**
 * Coarse "N units ago" bucket used to render relative timestamps without locale-specific
 * formatting. Callers map each variant to a localized string template.
 */
sealed interface RelativeTimeBucket {
    data object JustNow : RelativeTimeBucket

    data class Minutes(val value: Int) : RelativeTimeBucket

    data class Hours(val value: Int) : RelativeTimeBucket

    data class Days(val value: Int) : RelativeTimeBucket
}

fun relativeTimeBucket(deltaMillis: Long): RelativeTimeBucket {
    // Negative deltas (clock skew between server-stamped time and the local clock) collapse to
    // "just now"; rendering "-2m ago" would confuse and the magnitude is irrelevant.
    val clamped = if (deltaMillis < 0) 0 else deltaMillis
    val seconds = clamped / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> RelativeTimeBucket.JustNow
        minutes < 60 -> RelativeTimeBucket.Minutes(minutes.toInt())
        hours < 24 -> RelativeTimeBucket.Hours(hours.toInt())
        else -> RelativeTimeBucket.Days(days.toInt())
    }
}
