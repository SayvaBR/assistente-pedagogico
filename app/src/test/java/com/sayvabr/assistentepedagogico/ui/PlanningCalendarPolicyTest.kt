package com.sayvabr.assistentepedagogico.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningCalendarPolicyTest {
    @Test fun leapFebruaryHasAllDaysAndMondayFirstCells() {
        val cells = PlanningCalendarPolicy.monthCells(LocalDate.parse("2024-02-28"))
        assertEquals(35, cells.size)
        assertEquals(LocalDate.parse("2024-01-29"), cells.first())
        assertTrue(cells.contains(LocalDate.parse("2024-02-29")))
        assertEquals(LocalDate.parse("2024-03-03"), cells.last())
    }

    @Test fun monthsThatNeedSixRowsDoNotLoseDates() {
        val cells = PlanningCalendarPolicy.monthCells(LocalDate.parse("2026-08-17"))
        assertEquals(42, cells.size)
        assertEquals(LocalDate.parse("2026-07-27"), cells.first())
        assertEquals(LocalDate.parse("2026-09-06"), cells.last())
        assertTrue(cells.contains(LocalDate.parse("2026-08-31")))
    }

    @Test fun shortMonthsClampSelectedDayWithoutCrossingMonth() {
        assertEquals(LocalDate.parse("2025-02-28"), PlanningCalendarPolicy.shiftMonth(LocalDate.parse("2025-01-31"), 1))
        assertEquals(LocalDate.parse("2024-02-29"), PlanningCalendarPolicy.shiftMonth(LocalDate.parse("2024-01-31"), 1))
        assertEquals(LocalDate.parse("2027-01-31"), PlanningCalendarPolicy.shiftMonth(LocalDate.parse("2026-12-31"), 1))
    }

    @Test fun weekStartsMondayAcrossYearAndFiltersByIsoDay() {
        val focus = LocalDate.parse("2027-01-01")
        assertEquals(LocalDate.parse("2026-12-28"), PlanningCalendarPolicy.weekStart(focus))
        assertEquals(LocalDate.parse("2027-01-03"), PlanningCalendarPolicy.weekDays(focus).last())
        assertTrue(PlanningCalendarPolicy.matchesWeek("2026-12-31", focus))
        assertTrue(PlanningCalendarPolicy.matchesWeek("2027-01-03", focus))
        assertFalse(PlanningCalendarPolicy.matchesWeek("2027-01-04", focus))
        assertTrue(PlanningCalendarPolicy.matchesDay("2027-01-01", focus))
        assertFalse(PlanningCalendarPolicy.matchesDay("2027-01-02", focus))
        assertTrue(PlanningCalendarPolicy.matchesMonth("2027-01-31", focus))
        assertFalse(PlanningCalendarPolicy.matchesMonth("2026-12-31", focus))
    }

    @Test fun monthListingIncludesEntireSelectedMonthButNeverAdjacentCalendarCells() {
        val focus = LocalDate.parse("2026-08-17")
        // Grid has Jul 27 and Sep 06, but list may contain only dates belonging to August.
        assertTrue(PlanningCalendarPolicy.monthCells(focus).contains(LocalDate.parse("2026-07-27")))
        assertTrue(PlanningCalendarPolicy.monthCells(focus).contains(LocalDate.parse("2026-09-06")))
        assertTrue(PlanningCalendarPolicy.inPeriod("2026-08-01", focus, "Mês"))
        assertTrue(PlanningCalendarPolicy.inPeriod("2026-08-31", focus, "Mês"))
        assertFalse(PlanningCalendarPolicy.inPeriod("2026-07-31", focus, "Mês"))
        assertFalse(PlanningCalendarPolicy.inPeriod("2026-09-01", focus, "Mês"))
        assertFalse(PlanningCalendarPolicy.inPeriod("2026-08-18", focus, "Dia"))
        assertTrue(PlanningCalendarPolicy.inPeriod("2026-08-17", focus, "Dia"))
        assertTrue(PlanningCalendarPolicy.inPeriod("2026-08-23", focus, "Semana"))
        assertFalse(PlanningCalendarPolicy.inPeriod("2026-08-24", focus, "Semana"))
        assertFalse(PlanningCalendarPolicy.inPeriod("2026-08-17", focus, "Arquivados"))
    }
}
