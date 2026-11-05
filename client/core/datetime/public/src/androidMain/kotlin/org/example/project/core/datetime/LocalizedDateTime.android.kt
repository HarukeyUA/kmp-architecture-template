package org.example.project.core.datetime

import android.text.format.DateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.TimeZone

actual object LocalizedDateTime {
    // Detekt's type resolution does not see the reads inside this actual object.
    @Suppress("UnusedPrivateProperty")
    private val formatterCache = ConcurrentHashMap<String, DateTimeFormatter>()

    actual fun time(instant: Instant, zone: TimeZone): String = format(instant, zone, "jm")

    actual fun weekday(instant: Instant, zone: TimeZone): String = format(instant, zone, "EEEE")

    actual fun weekdayShort(instant: Instant, zone: TimeZone): String = format(instant, zone, "EEE")

    actual fun dayMonthFull(instant: Instant, zone: TimeZone): String =
        format(instant, zone, "MMMMd")

    actual fun dayMonthShort(instant: Instant, zone: TimeZone): String =
        format(instant, zone, "MMMd")

    actual fun dateFull(instant: Instant, zone: TimeZone): String = format(instant, zone, "yMMMMd")

    actual fun monthYear(instant: Instant, zone: TimeZone): String = format(instant, zone, "yMMMM")

    actual fun monthFull(instant: Instant, zone: TimeZone): String = format(instant, zone, "MMMM")

    actual fun monthShort(instant: Instant, zone: TimeZone): String = format(instant, zone, "MMM")

    private fun format(instant: Instant, zone: TimeZone, skeleton: String): String {
        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val cacheKey = "${locale.toLanguageTag()}-$skeleton"
        val formatter =
            formatterCache.getOrPut(cacheKey) {
                val pattern =
                    DateFormat.getBestDateTimePattern(locale, skeleton)?.takeIf { it.isNotBlank() }
                        ?: fallbackPattern(skeleton)
                DateTimeFormatter.ofPattern(pattern, locale)
            }
        return formatter.withZone(ZoneId.of(zone.id)).format(instant.toJavaInstant())
    }

    private fun fallbackPattern(skeleton: String): String =
        when (skeleton) {
            "jm" -> "HH:mm"
            "EEEE" -> "EEEE"
            "EEE" -> "EEE"
            "MMMMd" -> "MMMM d"
            "MMMd" -> "MMM d"
            "yMMMMd" -> "MMMM d, y"
            "yMMMM" -> "MMMM y"
            "MMMM" -> "MMMM"
            "MMM" -> "MMM"
            else -> skeleton
        }
}
