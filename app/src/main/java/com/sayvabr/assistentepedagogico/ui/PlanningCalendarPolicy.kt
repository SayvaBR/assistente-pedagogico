package com.sayvabr.assistentepedagogico.ui

import java.time.LocalDate
import java.time.YearMonth

/** One source of truth for calendar dates shared by Planning and Agenda.
 * ISO weeks begin on Monday; all dates remain ISO yyyy-MM-dd for SQLite filtering.
 */
internal object PlanningCalendarPolicy {
    fun weekStart(day: LocalDate): LocalDate = day.minusDays((day.dayOfWeek.value - 1).toLong())

    fun weekDays(day: LocalDate): List<LocalDate> =
        (0L..6L).map { weekStart(day).plusDays(it) }

    /** Full Monday-first calendar; include neighbor dates, never omit a sixth week. */
    fun monthCells(day: LocalDate): List<LocalDate> {
        val first = day.withDayOfMonth(1)
        val leading = first.dayOfWeek.value - 1
        val occupied = leading + first.lengthOfMonth()
        val cells = if (occupied <= 35) 35 else 42
        return (0 until cells).map { first.minusDays(leading.toLong()).plusDays(it.toLong()) }
    }

    /** Preserve the intended day where it exists and clamp for shorter months. */
    fun shiftMonth(day: LocalDate, months: Long): LocalDate {
        val target = YearMonth.from(day).plusMonths(months)
        return target.atDay(minOf(day.dayOfMonth, target.lengthOfMonth()))
    }

    fun shiftWeek(day: LocalDate, weeks: Long): LocalDate = day.plusWeeks(weeks)

    fun matchesDay(isoDate: String, selected: LocalDate): Boolean = isoDate == selected.toString()
    fun matchesWeek(isoDate: String, selected: LocalDate): Boolean =
        isoDate >= weekStart(selected).toString() && isoDate <= weekStart(selected).plusDays(6).toString()
    fun matchesMonth(isoDate: String, selected: LocalDate): Boolean =
        isoDate.startsWith(YearMonth.from(selected).toString() + "-")

    /** Calendar grid and its list must use the SAME period; adjacent grid dates do not leak into the month list. */
    fun inPeriod(isoDate: String, selected: LocalDate, mode: String): Boolean = when (mode) {
        "Dia" -> matchesDay(isoDate, selected)
        "Semana" -> matchesWeek(isoDate, selected)
        "Mês" -> matchesMonth(isoDate, selected)
        else -> false
    }
}
