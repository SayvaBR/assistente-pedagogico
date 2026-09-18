package com.sayvabr.assistentepedagogico.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure formatter: sharing only happens after an explicit action in the lesson editor.
 * Never includes students or any other classroom records, and never contacts a server.
 */
object LessonPlanShare {
    private val brazilianDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))

    fun asPlainText(input: LessonPlanV6.Input, classroomName: String): String {
        val plan = LessonPlanV6.validated(input)
        val room = classroomName.trim()
        require(room.isNotEmpty()) { "Selecione uma turma para compartilhar o plano." }
        val result = StringBuilder()
        fun line(label: String, value: String) {
            if (value.isNotBlank()) result.append(label).append(": ").append(value).append('\n')
        }
        fun section(heading: String) {
            result.append('\n').append(heading).append('\n')
        }
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
        // A third-party offline catalogue is still under official-document audit. Do not imply
        // that a manually entered code or catalogue description has been officially verified.
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
}
