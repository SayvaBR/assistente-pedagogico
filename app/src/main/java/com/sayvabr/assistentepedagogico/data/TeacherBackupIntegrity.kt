package com.sayvabr.assistentepedagogico.data

import org.json.JSONObject
import java.time.LocalTime

/** Additional semantic validation before the restore transaction is allowed to replace any records.
 * Foreign keys alone cannot detect a valid student ID belonging to the wrong classroom.
 */
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

        // A valid duration is not enough: importing a corrupted plan with impossible timings
        // would otherwise persist an inconsistent pedagogical schedule in SQLite.
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
        }

        // Older backups legitimately do not know an appointment's duration (end == start).
        // A recorded end before start, however, must never be restored as a valid appointment.
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
