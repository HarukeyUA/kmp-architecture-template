package org.example.project.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * A calendar month identified by its year and month-of-year. Comparable in chronological order so
 * months can be ordered, range-checked, and used as map keys without converting to dates.
 */
@Suppress("DataClassContainsFunctions")
data class YearMonth(val year: Int, val month: Month) : Comparable<YearMonth> {

    /** First day of the month, a `LocalDate` anchor for grid layout. */
    val firstDay: LocalDate
        get() = LocalDate(year, month, 1)

    /** Inclusive last day of the month. */
    val lastDay: LocalDate
        get() = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    /** Number of days in the month, accounting for leap years. */
    val length: Int
        get() = firstDay.daysUntil(firstDay.plus(1, DateTimeUnit.MONTH))

    fun plusMonths(months: Int): YearMonth =
        firstDay.plus(months, DateTimeUnit.MONTH).let { YearMonth(it.year, it.month) }

    fun minusMonths(months: Int): YearMonth = plusMonths(-months)

    override fun compareTo(other: YearMonth): Int {
        val byYear = year.compareTo(other.year)
        return if (byYear == 0) month.number.compareTo(other.month.number) else byYear
    }

    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.number.toString().padStart(2, '0')}"

    companion object {
        fun from(date: LocalDate): YearMonth = YearMonth(date.year, date.month)

        /**
         * Parses the canonical `yyyy-MM` form produced by [toString]. Throws on malformed input,
         * since restoring an unparseable value is a storage corruption bug, not a UI fallback case.
         */
        fun parse(text: String): YearMonth {
            val dash = text.indexOf('-')
            require(dash > 0 && dash < text.length - 1) { "Invalid YearMonth: '$text'" }
            return YearMonth(
                text.substring(0, dash).toInt(),
                Month(text.substring(dash + 1).toInt()),
            )
        }
    }
}

/** Number of whole months from `this` (inclusive) to [other] (inclusive); negative if reversed. */
operator fun YearMonth.minus(other: YearMonth): Int =
    (year - other.year) * 12 + (month.number - other.month.number)

/** Inclusive month range, used for windowed pager queries. */
@Suppress("DataClassContainsFunctions")
data class YearMonthRange(val start: YearMonth, val endInclusive: YearMonth) : Iterable<YearMonth> {

    init {
        require(start <= endInclusive) { "start ($start) must be <= endInclusive ($endInclusive)" }
    }

    override fun iterator(): Iterator<YearMonth> = iterator {
        var current = start
        while (current <= endInclusive) {
            yield(current)
            current = current.plusMonths(1)
        }
    }

    /** O(1) membership check (shadows the linear [Iterable.contains] extension). */
    operator fun contains(value: YearMonth): Boolean = value >= start && value <= endInclusive

    /** Number of months in the range, inclusive of both endpoints. */
    val count: Int
        get() =
            (endInclusive.year - start.year) * 12 +
                (endInclusive.month.number - start.month.number) +
                1
}
