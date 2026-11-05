package org.example.project.core.datetime

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId

// ImplicitDefaultLocale: every formatter here is built with an explicit locale already.
@Suppress("ImplicitDefaultLocale")
actual object LocalizedDateTime {
    // Detekt's type resolution does not see the reads inside this actual object.
    @Suppress("UnusedPrivateProperty")
    private val skeletonCache = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

    private fun skeletonFormatter(skeleton: String): DateTimeFormatter {
        val locale = Locale.getDefault()
        return skeletonCache.getOrPut(skeleton to locale) {
            DateTimeFormatter.ofLocalizedPattern(skeleton).withLocale(locale)
        }
    }

    actual fun time(instant: Instant, zone: TimeZone): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun weekday(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("EEEE").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun weekdayShort(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("EEE").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun dayMonthFull(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("MMMMd").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun dayMonthShort(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("MMMd").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun dateFull(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("yMMMMd").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun monthYear(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("yMMMM").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun monthFull(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("MMMM").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    actual fun monthShort(instant: Instant, zone: TimeZone): String =
        skeletonFormatter("MMM").format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))
}
