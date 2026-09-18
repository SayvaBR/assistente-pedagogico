package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonScheduleWindowTest {
    private fun plan(time: String, duration: Int) = LessonPlanV6.Input(
        title = "Experimento sobre a água",
        subject = "Ciências",
        day = "2026-09-22",
        time = time,
        durationMinutes = duration,
        objective = "Observar transformações da água.",
        content = "Ciclo da água",
        method = "Experimentação orientada",
    )

    @Test fun computesExpectedFinishForOrdinaryLesson() {
        assertEquals("09:45", LessonPlanV6.endTime("09:00", 45))
        assertEquals("17:00", LessonPlanV6.endTime("16:10", 50))
        assertEquals("09:45", LessonPlanV6.endTime(LessonPlanV6.validated(plan("09:00", 45)).time, 45))
    }

    @Test fun refusesToWrapMidnightWithoutWarning() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(plan("23:45", 50))
        }
        assertTrue(error.message.orEmpty().contains("fim do dia"))
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(plan("23:50", 10))
        }
    }

    @Test fun permitsLastLessonEndingBeforeMidnight() {
        assertEquals("23:59", LessonPlanV6.endTime("23:49", 10))
        assertEquals("23:59", LessonPlanV6.endTime(LessonPlanV6.validated(plan("23:49", 10)).time, 10))
    }

    @Test fun refusesMalformedTimesAndDurations() {
        listOf("9:00", "24:00", "08:99", "08:00:00", "abc").forEach { invalid ->
            assertThrows("Invalid time: $invalid", IllegalArgumentException::class.java) {
                LessonPlanV6.endTime(invalid, 50)
            }
        }
        listOf(-1, 0, 9, 481).forEach { invalid ->
            assertThrows("Invalid duration: $invalid", IllegalArgumentException::class.java) {
                LessonPlanV6.endTime("08:00", invalid)
            }
        }
    }
}
