package com.sayvabr.assistentepedagogico.data

import org.json.JSONObject
import java.time.LocalTime

/** Semantic validation before the restore transaction replaces any records. */
internal object TeacherBackupIntegrity {
    fun validate(root: JSONObject) {
        val students = root.getJSONArray("students")
        val studentClassrooms = mutableMapOf<Long, Long>()
        for (index in 0 until students.length()) {
            val student = students.getJSONObject(index)
            val id = student.getLong("id")
            val classroomId = student.getLong("classroomId")
            require(studentClassrooms.put(id, classroomId) == null) {
                "Backup inválido: há identificadores de alunos duplicados."
            }
        }

        val attendance = root.getJSONArray("attendance")
        for (index in 0 until attendance.length()) {
            val entry = attendance.getJSONObject(index)
            val owner = studentClassrooms[entry.getLong("studentId")]
            require(owner == entry.getLong("classroomId")) {
                "Frequência vinculada a uma turma diferente da turma do aluno."
            }
        }

        val observations = root.getJSONArray("observations")
        for (index in 0 until observations.length()) {
            val entry = observations.getJSONObject(index)
            if (!entry.isNull("studentId")) {
                val owner = studentClassrooms[entry.getLong("studentId")]
                require(owner == entry.getLong("classroomId")) {
                    "Observação vinculada a uma turma diferente da turma do aluno."
                }
            }
        }

        // Validate timing and states BEFORE restoring a backup.
        val lessons = root.getJSONArray("lessons")
        for (index in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(index)
            val duration = lesson.optInt("durationMinutes", 50)
            val opening = lesson.optLong("openingMinutes", 0L)
            val development = lesson.optLong("developmentMinutes", 0L)
            val closing = lesson.optLong("closingMinutes", 0L)
            require(duration in 10..480 && listOf(opening, development, closing).all { it in 0..480 }) {
                "Duração ou momentos de aula inválidos no backup."
            }
            require(opening + development + closing <= duration.toLong()) {
                "A soma dos momentos ultrapassa a duração do plano no backup."
            }
            listOf(
                "abertura" to ("opening" to opening),
                "desenvolvimento" to ("development" to development),
                "fechamento" to ("closing" to closing),
            ).forEach { (label, fieldAndMinutes) ->
                val (field, minutes) = fieldAndMinutes
                require(lesson.optString(field, "").isBlank() == (minutes == 0L)) {
                    "O momento de $label deve ter descrição e tempo juntos no backup."
                }
            }
            val legacyArchived = lesson.optBoolean("archived", false)
            val status = if (lesson.has("pedagogicalStatus")) LessonStatus.parse(lesson.getString("pedagogicalStatus"))
                else if (legacyArchived) LessonStatus.ARCHIVED else LessonStatus.DRAFT
            val previous = if (lesson.has("statusBeforeArchive")) LessonStatus.parse(lesson.getString("statusBeforeArchive"))
                else LessonStatus.DRAFT
            require(previous != LessonStatus.ARCHIVED && legacyArchived == (status == LessonStatus.ARCHIVED)) {
                "Estado pedagógico e arquivamento inconsistentes no backup."
            }
            val effective = if (status == LessonStatus.ARCHIVED) previous else status
            if (effective == LessonStatus.READY || effective == LessonStatus.COMPLETED) {
                require(listOf("title", "subject", "objective", "content", "method", "opening", "development", "closing")
                    .all { lesson.optString(it, "").isNotBlank() } && listOf(opening, development, closing).all { it > 0L }) {
                    "Plano pronto ou concluído está incompleto no backup."
                }
            }

            // A legacy v5 plan never had duration; retain otherwise-unenriched late-day records.
            val isLegacyDefault = duration == 50 && opening == 0L && development == 0L && closing == 0L &&
                listOf("specificObjectives", "bnccCodes", "justification", "opening", "development",
                    "closing", "assessment", "adaptations").all { lesson.optString(it, "").isBlank() }
            if (lesson.has("durationMinutes") && !lesson.isNull("durationMinutes") && !isLegacyDefault) {
                LessonPlanV6.endTime(lesson.getString("time"), duration)
            }
        }

        val knownClassrooms = mutableSetOf<Long>()
        root.getJSONArray("classrooms").let { rooms ->
            for (index in 0 until rooms.length()) {
                require(knownClassrooms.add(rooms.getJSONObject(index).getLong("id"))) {
                    "Backup contém turmas com IDs duplicados."
                }
            }
        }
        val lessonOwners = mutableMapOf<Long, Long>()
        for (index in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(index)
            require(lessonOwners.put(lesson.getLong("id"), lesson.getLong("classroomId")) == null) {
                "Backup contém planos com IDs duplicados."
            }
        }
        val activities = root.getJSONArray("activities")
        val activityIds = mutableSetOf<Long>()
        for (index in 0 until activities.length()) {
            val activity = activities.getJSONObject(index)
            val id = activity.getLong("id")
            val classroomId = activity.getLong("classroomId")
            val lessonId = if (activity.isNull("lessonId")) null else activity.getLong("lessonId")
            require(id > 0 && activityIds.add(id)) { "Backup contém atividades com IDs inválidos ou duplicados." }
            require(classroomId in knownClassrooms) { "Atividade vinculada a turma inexistente no backup." }
            require(lessonId == null || lessonOwners[lessonId] == classroomId) {
                "Atividade vinculada a plano de outra turma ou inexistente no backup."
            }
            LessonActivityV7.validated(LessonActivityV7.Input(
                classroomId = classroomId,
                lessonId = lessonId,
                title = activity.getString("title"),
                instructions = activity.getString("instructions"),
                durationMinutes = activity.getInt("durationMinutes"),
            ))
        }

        val appointments = root.getJSONArray("appointments")
        for (index in 0 until appointments.length()) {
            val appointment = appointments.getJSONObject(index)
            if (!appointment.isNull("endTime")) {
                require(LocalTime.parse(appointment.getString("endTime")) >=
                    LocalTime.parse(appointment.getString("time"))) {
                    "Compromisso com horário final anterior ao inicial no backup."
                }
            }
        }
    }
}
