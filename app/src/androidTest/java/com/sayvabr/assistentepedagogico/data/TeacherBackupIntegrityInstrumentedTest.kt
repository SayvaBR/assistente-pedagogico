package com.sayvabr.assistentepedagogico.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic JSON only: validates restore previews without opening or deleting any database. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupIntegrityInstrumentedTest {
    private fun classRecord(id: Long) = JSONObject()
        .put("id", id).put("name", "Turma fictícia $id")
        .put("stage", "Ensino Fundamental").put("shift", "Matutino")

    private fun validBackup(): JSONObject = JSONObject().apply {
        put("format", TeacherBackupCodec.FORMAT)
        put("version", TeacherBackupCodec.VERSION)
        put("profile", JSONObject.NULL)
        put("classrooms", JSONArray().put(classRecord(1)).put(classRecord(2)))
        put("students", JSONArray().put(JSONObject()
            .put("id", 7).put("classroomId", 1).put("name", "Estudante fictício")))
        put("lessons", JSONArray().put(JSONObject()
            .put("id", 3).put("classroomId", 1).put("title", "Aula fictícia")
            .put("subject", "Ciências").put("date", "2026-09-22").put("time", "08:00")
            .put("durationMinutes", 50).put("objective", "Investigar o assunto.")
            .put("content", "Experimento").put("method", "Observação")
            .put("openingMinutes", 10).put("developmentMinutes", 30).put("closingMinutes", 10)))
        put("attendance", JSONArray().put(JSONObject().put("studentId", 7).put("classroomId", 1)
            .put("date", "2026-09-22").put("status", "P")))
        put("observations", JSONArray().put(JSONObject().put("id", 8).put("studentId", 7)
            .put("classroomId", 1).put("kind", "Aprendizagem")
            .put("body", "Registro fictício").put("date", "2026-09-22")))
        put("appointments", JSONArray().put(JSONObject().put("id", 9).put("title", "Reunião fictícia")
            .put("date", "2026-09-22").put("time", "10:00").put("endTime", "11:00")))
        put("files", JSONArray())
        put("folders", JSONArray())
    }

    @Test fun acceptsConsistentBackupAndLegacyUnknownAppointmentDuration() {
        val root = validBackup()
        assertEquals(1, TeacherBackupRestore.preview(root.toString()).attendance)
        root.getJSONArray("appointments").getJSONObject(0).put("endTime", "10:00")
        assertEquals(1, TeacherBackupRestore.preview(root.toString()).appointments)
    }

    @Test fun rejectsAttendanceAssignedToWrongClassEvenWhenBothIdsExist() {
        val root = validBackup()
        root.getJSONArray("attendance").getJSONObject(0).put("classroomId", 2)
        val error = assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(root.toString())
        }
        assertTrue(error.message.orEmpty().contains("Frequência"))
    }

    @Test fun rejectsObservationAssignedToWrongClassEvenWhenBothIdsExist() {
        val root = validBackup()
        root.getJSONArray("observations").getJSONObject(0).put("classroomId", 2)
        val error = assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(root.toString())
        }
        assertTrue(error.message.orEmpty().contains("Observação"))
    }

    @Test fun rejectsLessonMomentsThatExceedDuration() {
        val root = validBackup()
        root.getJSONArray("lessons").getJSONObject(0).put("developmentMinutes", 50)
        val error = assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(root.toString())
        }
        assertTrue(error.message.orEmpty().contains("momentos"))
    }

    @Test fun rejectsAppointmentEndingBeforeItsStart() {
        val root = validBackup()
        root.getJSONArray("appointments").getJSONObject(0).put("endTime", "09:00")
        val error = assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(root.toString())
        }
        assertTrue(error.message.orEmpty().contains("Compromisso"))
    }
}
