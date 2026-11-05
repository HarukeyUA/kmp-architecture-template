package org.example.project.core.datetime

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * Locale-aware date/time formatting. Each platform delegates to its own date-formatting APIs
 * (`DateFormat.getBestDateTimePattern` on Android, `NSDateFormatter` on iOS, `DateTimeFormatter` on
 * JVM) so the rendered strings track the device's current locale and preferences (24h/12h,
 * weekday/month order, language).
 */
expect object LocalizedDateTime {
    /**
     * Hour + minute in the locale's preferred form (e.g. `15:42` in en-GB / fr, `3:42 PM` in
     * en-US).
     */
    fun time(instant: Instant, zone: TimeZone): String

    /** Standalone weekday name (e.g. `Thursday` in en, `jeudi` in fr). */
    fun weekday(instant: Instant, zone: TimeZone): String

    /** Short standalone weekday name (e.g. `Thu` in en, `jeu.` in fr). */
    fun weekdayShort(instant: Instant, zone: TimeZone): String

    /** Day-of-month with full month name (e.g. `20 April` in en-GB, `April 20` in en-US). */
    fun dayMonthFull(instant: Instant, zone: TimeZone): String

    /** Day-of-month with short month name (e.g. `20 Apr` in en-GB, `Apr 20` in en-US). */
    fun dayMonthShort(instant: Instant, zone: TimeZone): String

    /** Full date with year (e.g. `20 April 2026` in en-GB, `April 20, 2026` in en-US). */
    fun dateFull(instant: Instant, zone: TimeZone): String

    /** Full month and year (e.g. `April 2026` in en, `avril 2026` in fr). */
    fun monthYear(instant: Instant, zone: TimeZone): String

    /** Standalone month name without year (e.g. `April` in en, `avril` in fr). */
    fun monthFull(instant: Instant, zone: TimeZone): String

    /** Short standalone month name (e.g. `Apr` in en, `avr.` in fr). */
    fun monthShort(instant: Instant, zone: TimeZone): String
}
