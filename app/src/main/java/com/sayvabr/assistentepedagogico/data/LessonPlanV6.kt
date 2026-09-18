package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Additive lesson-plan evolution. Existing plans remain valid and are never rewritten during migration. */
object LessonPlanV6 {
    const val VERSION = 6

    data class Input(
        val title: String,
        val subject: String,
        val day: String,
        val time: String,
        val durationMinutes: Int,
        val objective: String,
        val specificObjectives: String = "",
        val content: String,
        val bnccCodes: String = "",
        val justification: String = "",
        val method: String,
        val opening: String = "",
        val openingMinutes: Int = 0,
        val development: String = "",
        val developmentMinutes: Int = 0,
        val closing: String = "",
        val closingMinutes: Int = 0,
        val assessment: String = "",
        val adaptations: String = "",
        /** UI command routed by the Activity's existing save/commit callback; never stored as lesson content. */
        val statusTransition: LessonStatus? = null,
        /** Optional v9 presentation; canonical fields remain stored once in lessons. */
        val composition: PlanComposition? = null,
    )

    /** The editor and persistence validation share the same calculation. No silent midnight wrap. */
    fun endTime(time: String, durationMinutes: Int): String {
        require(Regex("[0-9]{2}:[0-9]{2}").matches(time)) { "Informe o horário inicial no formato HH:MM." }
        val start = runCatching { LocalTime.parse(time) }.getOrElse {
            throw IllegalArgumentException("Informe um horário inicial válido.", it)
        }
        require(durationMinutes in 10..480) { "A duração deve ficar entre 10 minutos e 8 horas." }
        val endMinutes = start.hour * 60 + start.minute + durationMinutes
        require(endMinutes < 24 * 60) { "A aula terminaria após o fim do dia. Ajuste o início ou a duração." }
        return LocalTime.of(endMinutes / 60, endMinutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    fun validated(input: Input): Input {
        require(input.statusTransition == null) { "Transições de estado devem usar a operação específica do plano existente." }
        val title = input.title.trim()
        val subject = input.subject.trim()
        val objective = input.objective.trim()
        val content = input.content.trim()
        val method = input.method.trim()
        require(title.length >= 3) { "Informe um título de aula com pelo menos 3 caracteres." }
        require(subject.isNotEmpty()) { "Informe o componente curricular." }
        LocalDate.parse(input.day)
        endTime(input.time, input.durationMinutes)
        require(objective.length >= 5) { "Descreva o objetivo geral da aula." }
        require(content.isNotEmpty()) { "Informe o conteúdo ou objeto de conhecimento." }
        require(method.isNotEmpty()) { "Informe a metodologia ou estratégia de ensino." }

        // Partial drafts may omit a whole moment, but must never persist a description without
        // time or a time allocation without instructions. Bound each value before summing to
        // prevent Int overflow from manually entered durations.
        listOf(
            Triple("abertura", input.opening, input.openingMinutes),
            Triple("desenvolvimento", input.development, input.developmentMinutes),
            Triple("fechamento", input.closing, input.closingMinutes),
        ).forEach { (name, description, minutes) ->
            require(minutes in 0..input.durationMinutes) { "O tempo de $name deve estar entre 0 e a duração total da aula." }
            require(description.isBlank() == (minutes == 0)) {
                "Preencha a descrição e o tempo de $name juntos, ou deixe ambos vazios."
            }
        }
        val momentTotal = input.openingMinutes + input.developmentMinutes + input.closingMinutes
        require(momentTotal <= input.durationMinutes) {
            "A soma dos momentos não pode ultrapassar a duração total da aula."
        }
        val codes = input.bnccCodes.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        require(codes.distinct().size == codes.size) { "Remova códigos BNCC duplicados." }
        return input.copy(
            title = title,
            subject = subject,
            objective = objective,
            specificObjectives = input.specificObjectives.trim(),
            content = content,
            bnccCodes = codes.joinToString(", "),
            justification = input.justification.trim(),
            method = method,
            opening = input.opening.trim(),
            development = input.development.trim(),
            closing = input.closing.trim(),
            assessment = input.assessment.trim(),
            adaptations = input.adaptations.trim(),
        )
    }

    fun migrate(db: SQLiteDatabase) {
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(lessons)", null).use { c ->
            val name = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) columns += c.getString(name)
        }
        fun add(name: String, sql: String) {
            if (name !in columns) {
                db.execSQL("ALTER TABLE lessons ADD COLUMN $sql")
                columns += name
            }
        }
        add("duration_minutes", "duration_minutes INTEGER NOT NULL DEFAULT 50")
        add("specific_objectives", "specific_objectives TEXT NOT NULL DEFAULT ''")
        add("bncc_codes", "bncc_codes TEXT NOT NULL DEFAULT ''")
        add("justification", "justification TEXT NOT NULL DEFAULT ''")
        add("opening", "opening TEXT NOT NULL DEFAULT ''")
        add("opening_minutes", "opening_minutes INTEGER NOT NULL DEFAULT 0")
        add("development", "development TEXT NOT NULL DEFAULT ''")
        add("development_minutes", "development_minutes INTEGER NOT NULL DEFAULT 0")
        add("closing", "closing TEXT NOT NULL DEFAULT ''")
        add("closing_minutes", "closing_minutes INTEGER NOT NULL DEFAULT 0")
        add("assessment", "assessment TEXT NOT NULL DEFAULT ''")
        add("adaptations", "adaptations TEXT NOT NULL DEFAULT ''")
    }
}
