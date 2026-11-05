package org.example.project.core.datetime

import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.defaultTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.timeZoneWithName

@ThreadLocal private val formatterCache = mutableMapOf<String, NSDateFormatter>()

actual object LocalizedDateTime {
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

    private fun format(instant: Instant, zone: TimeZone, template: String): String {
        val locale = NSLocale.currentLocale
        val cacheKey = "${locale.localeIdentifier}-$template"
        val formatter =
            formatterCache.getOrPut(cacheKey) {
                NSDateFormatter().apply {
                    this.locale = locale
                    setLocalizedDateFormatFromTemplate(template)
                }
            }
        // Time zone is per-call: swapping it on a cached formatter is cheaper than recreating it
        // and keeps the cache key from growing with every zone seen.
        formatter.timeZone = NSTimeZone.timeZoneWithName(zone.id) ?: NSTimeZone.defaultTimeZone
        val seconds =
            instant.epochSeconds.toDouble() +
                (instant.nanosecondsOfSecond.toDouble() / 1_000_000_000.0)
        val date = NSDate.dateWithTimeIntervalSince1970(seconds)
        return formatter.stringFromDate(date)
    }
}
