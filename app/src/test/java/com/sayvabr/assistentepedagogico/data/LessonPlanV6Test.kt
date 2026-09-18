package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LessonPlanV6Test {
    private fun base() = LessonPlanV6.Input(
        title = "Leitura e interpretação",
        subject = "Língua Portuguesa",
        day = "2026-09-18",
        time = "08:00",
        durationMinutes = 50,
        objective = "Compreender informações explícitas em um texto.",
        content = "Leitura e interpretação de textos",
        method = "Leitura compartilhada e discussão guiada",
    )

    @Test fun acceptsProfessionalPlanAndNormalizesBnccList() {
        val value = LessonPlanV6.validated(base().copy(
            bnccCodes = "EF15LP03; EF15LP04",
            opening = "Retomar a aula anterior",
            openingMinutes = 10,
            development = "Leitura e atividade em dupla",
            developmentMinutes = 30,
            closing = "Síntese coletiva",
            closingMinutes = 10,
        ))
        assertEquals("EF15LP03, EF15LP04", value.bnccCodes)
        assertEquals(50, value.openingMinutes + value.developmentMinutes + value.closingMinutes)
    }

    @Test fun rejectsMomentsLongerThanLesson() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(openingMinutes = 20, developmentMinutes = 30, closingMinutes = 10))
        }
    }

    @Test fun rejectsDuplicateBnccCodes() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(bnccCodes = "EF15LP03, EF15LP03"))
        }
    }

    @Test fun rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(durationMinutes = 5))
        }
    }
}