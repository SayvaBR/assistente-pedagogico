package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
            LessonPlanV6.validated(base().copy(
                opening = "Introdução", openingMinutes = 20,
                development = "Leitura", developmentMinutes = 30,
                closing = "Síntese", closingMinutes = 10,
            ))
        }
    }

    @Test fun rejectsDescriptionWithoutDuration() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(opening = "Retomar a aula anterior", openingMinutes = 0))
        }
        assertTrue(exception.message.orEmpty().contains("abertura"))
    }

    @Test fun rejectsDurationWithoutDescription() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(development = "   ", developmentMinutes = 30))
        }
        assertTrue(exception.message.orEmpty().contains("desenvolvimento"))
    }

    @Test fun acceptsOmittedMomentsForLegacyAndPartialPlans() {
        val plan = LessonPlanV6.validated(base())
        assertEquals(0, plan.openingMinutes + plan.developmentMinutes + plan.closingMinutes)
    }

    @Test fun rejectsHugeDurationBeforeSumOverflow() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanV6.validated(base().copy(
                opening = "Início", openingMinutes = Int.MAX_VALUE,
                development = "Parte principal", developmentMinutes = Int.MAX_VALUE,
            ))
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
