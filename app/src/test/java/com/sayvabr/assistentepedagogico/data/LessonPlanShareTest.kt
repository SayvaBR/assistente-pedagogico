package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPlanShareTest {
    private fun input() = LessonPlanV6.Input(
        title = "  Ciclo da água  ",
        subject = "Ciências",
        day = "2026-09-22",
        time = "13:00",
        durationMinutes = 60,
        objective = "Investigar as mudanças de estado da água.",
        specificObjectives = "Observar a condensação.",
        content = "Ciclo da água",
        bnccCodes = "EF05CI02",
        method = "Experimento e discussão coletiva.",
        opening = "Retomar a aula anterior.",
        openingMinutes = 10,
        development = "Realizar o experimento.",
        developmentMinutes = 40,
        closing = "Registrar conclusões.",
        closingMinutes = 10,
        assessment = "Analisar registros produzidos.",
        adaptations = "Materiais ampliados quando necessário.",
    )

    @Test fun rendersReadableValidatedPlanWithoutOtherClassroomRecords() {
        val text = LessonPlanShare.asPlainText(input(), "  5º Ano A  ")
        assertTrue(text.startsWith("PLANO DE AULA — PRÉVIA PARA COMPARTILHAMENTO"))
        assertTrue(text.contains("Título: Ciclo da água\n"))
        assertTrue(text.contains("Turma: 5º Ano A\n"))
        assertTrue(text.contains("Data: 22/09/2026\n"))
        assertTrue(text.contains("Horário: 13:00–14:00\n"))
        assertTrue(text.contains("Abertura (10 min): Retomar a aula anterior."))
        assertTrue(text.contains("Desenvolvimento (40 min): Realizar o experimento."))
        assertTrue(text.contains("Fechamento (10 min): Registrar conclusões."))
        assertTrue(text.contains("Códigos BNCC informados (conferir com a fonte oficial): EF05CI02"))
        assertTrue(text.contains("Adaptações e acessibilidade: Materiais ampliados"))
        assertFalse(text.contains("null"))
    }

    @Test fun omitsEmptyOptionalSectionsButKeepsEssentialContent() {
        val text = LessonPlanShare.asPlainText(input().copy(
            specificObjectives = "", bnccCodes = "", opening = "", openingMinutes = 0,
            development = "", developmentMinutes = 0, closing = "", closingMinutes = 0,
            assessment = "", adaptations = "",
        ), "5º Ano A")
        assertTrue(text.contains("Objetivo geral: Investigar"))
        assertFalse(text.contains("Objetivos específicos:"))
        assertFalse(text.contains("Códigos BNCC informados"))
        assertFalse(text.contains("Abertura ("))
        assertFalse(text.contains("Avaliação / evidências:"))
    }

    @Test fun refusesSharingInvalidUnsavedDraftAndMissingClassroom() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanShare.asPlainText(input().copy(openingMinutes = 80), "5º Ano A")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LessonPlanShare.asPlainText(input(), "   ")
        }
    }

    @Test fun personalizedExportUsesExactOrderAndNeverFabricatesBnccText() {
        val layout = PlanComposition.standard()
            .move("bncc", -1)
            .add(PlanBlock("school_note", PlanBlockKind.CUSTOM, "Anotações da escola", "Texto livre fictício"), "identification")
            .move("school_note", -1)
        val text = LessonPlanShare.asPlainText(input(), "Turma fictícia", layout)
        assertTrue(text.indexOf("Anotações da escola") < text.indexOf("Identificação"))
        assertTrue(text.indexOf("Identificação") < text.indexOf("Habilidades BNCC"))
        assertTrue(text.contains("Texto livre fictício"))
        assertTrue(text.contains("Códigos BNCC informados (conferir com a fonte oficial)"))
        assertFalse(text.contains("Texto oficial gerado"))
        val without = layout.remove("bncc")
        assertFalse(LessonPlanShare.asPlainText(input(), "Turma fictícia", without).contains("EF05CI02"))
        assertEquals(layout.blocks.map { it.id }, PlanLayoutV9.decode(PlanLayoutV9.encode(layout)).blocks.map { it.id })
    }
}
