package com.sayvabr.assistentepedagogico.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure formatter: sharing happens only on the teacher's explicit action and never sends class records. */
object LessonPlanShare {
    private val brazilianDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

    /** Original plain-text layout stays source-compatible for legacy lesson screens and tests. */
    fun asPlainText(input: LessonPlanV6.Input, classroomName: String): String {
        val plan = LessonPlanV6.validated(input)
        val room = classroomName.trim()
        require(room.isNotEmpty()) { "Selecione uma turma para compartilhar o plano." }
        val result = StringBuilder()
        fun line(label: String, value: String) {
            if (value.isNotBlank()) result.append(label).append(": ").append(value).append('\n')
        }
        fun section(heading: String) { result.append('\n').append(heading).append('\n') }
        fun moment(heading: String, description: String, minutes: Int) {
            if (description.isNotBlank()) line("$heading ($minutes min)", description)
        }

        result.append("PLANO DE AULA — PRÉVIA PARA COMPARTILHAMENTO\n")
        line("Título", plan.title)
        line("Turma", room)
        line("Componente curricular", plan.subject)
        line("Data", LocalDate.parse(plan.day).format(brazilianDate))
        line("Horário", "${plan.time}–${LessonPlanV6.endTime(plan.time, plan.durationMinutes)}")
        line("Duração", "${plan.durationMinutes} min")

        section("O QUE VOU ENSINAR")
        line("Objetivo geral", plan.objective)
        line("Objetivos específicos", plan.specificObjectives)
        line("Conteúdo / objeto de conhecimento", plan.content)
        line("Códigos BNCC informados (conferir com a fonte oficial)", plan.bnccCodes)
        line("Justificativa / contextualização", plan.justification)

        section("COMO VOU DAR A AULA")
        line("Metodologia / estratégia", plan.method)
        moment("Abertura", plan.opening, plan.openingMinutes)
        moment("Desenvolvimento", plan.development, plan.developmentMinutes)
        moment("Fechamento", plan.closing, plan.closingMinutes)

        section("ACOMPANHAMENTO")
        line("Avaliação / evidências", plan.assessment)
        line("Adaptações e acessibilidade", plan.adaptations)
        return result.toString().trimEnd() + "\n"
    }

    /** A second explicit format respects the exact section order selected by the teacher. */
    fun asPlainText(input: LessonPlanV6.Input, classroomName: String, layout: PlanComposition): String {
        val plan = LessonPlanV6.validated(input)
        val room = classroomName.trim()
        require(room.isNotEmpty()) { "Selecione uma turma para compartilhar o plano." }
        // Validate the layout again before exposing a document outside the app.
        val blocks = PlanLayoutV9.decode(PlanLayoutV9.encode(layout)).blocks
        val result = StringBuilder("PLANO DE AULA — PRÉVIA PARA COMPARTILHAMENTO\n")
        fun line(label: String, value: String) {
            if (value.isNotBlank()) result.append(label).append(": ").append(value).append('\n')
        }
        blocks.forEach { block ->
            val lines = StringBuilder()
            fun emit(label: String, value: String) {
                if (value.isNotBlank()) lines.append(label).append(": ").append(value).append('\n')
            }
            when (block.kind) {
                PlanBlockKind.IDENTIFICATION -> {
                    emit("Título", plan.title)
                    emit("Turma", room)
                    emit("Componente curricular", plan.subject)
                    emit("Data", LocalDate.parse(plan.day).format(brazilianDate))
                    emit("Horário", "${plan.time}–${LessonPlanV6.endTime(plan.time, plan.durationMinutes)}")
                    emit("Duração", "${plan.durationMinutes} min")
                }
                PlanBlockKind.OBJECTIVES -> { emit("Objetivo geral", plan.objective); emit("Objetivos específicos", plan.specificObjectives) }
                PlanBlockKind.CONTENT -> emit("Conteúdo / objeto de conhecimento", plan.content)
                PlanBlockKind.BNCC -> emit("Códigos BNCC informados (conferir com a fonte oficial)", plan.bnccCodes)
                PlanBlockKind.CONTEXT -> emit("Justificativa / contextualização", plan.justification)
                PlanBlockKind.METHODOLOGY -> emit("Metodologia / estratégia", plan.method)
                PlanBlockKind.OPENING -> emit("Abertura (${plan.openingMinutes} min)", plan.opening)
                PlanBlockKind.DEVELOPMENT -> emit("Desenvolvimento (${plan.developmentMinutes} min)", plan.development)
                PlanBlockKind.CLOSING -> emit("Fechamento (${plan.closingMinutes} min)", plan.closing)
                PlanBlockKind.ASSESSMENT -> emit("Avaliação / evidências", plan.assessment)
                PlanBlockKind.ADAPTATIONS -> emit("Adaptações e acessibilidade", plan.adaptations)
                PlanBlockKind.RESOURCES -> Unit // No canonical resources field exists yet; do not fabricate it.
                PlanBlockKind.CUSTOM -> if (block.body.isNotBlank()) lines.append(block.body).append('\n')
            }
            if (lines.isNotEmpty()) result.append('\n').append(block.title).append('\n').append(lines)
        }
        return result.toString().trimEnd() + "\n"
    }
}
