package com.sayvabr.assistentepedagogico.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeacherBackupCodecInstrumentedTest {
    private fun snapshot() = TeacherSnapshot(
        profile = TeacherProfile("Docente Teste"),
        classrooms = listOf(Classroom(7, "Turma Sintética", "Ensino Fundamental", "Manhã")),
        students = listOf(Student(8, 7, "Aluno Sintético")),
        lessons = listOf(Lesson(9, 7, "Frações", "Matemática", "2026-09-18", "08:00", "Compreender frações", "Metades", "Materiais concretos")),
        attendance = listOf(Attendance(7, 8, "2026-09-18", "P")),
        observations = listOf(Observation(10, 7, 8, "Aprendizagem", "Registro exclusivamente sintético.", "2026-09-18", false)),
        appointments = listOf(Appointment(11, "Reunião sintética", "2026-09-19", "09:30")),
        files = listOf(SavedFile(12, "material.pdf", "content://synthetic/material", 13, true, null, "available")),
        folders = listOf(FileFolder(13, "Materiais")),
    )

    @Test fun roundTripEnvelopePreservesPedagogicalRecordsAndRevokesPortableSafAccess() {
        val payload = TeacherBackupCodec.encode(snapshot())
        val root = TeacherBackupCodec.validate(payload)
        assertEquals(TeacherBackupCodec.FORMAT, root.getString("format"))
        assertEquals("Turma Sintética", root.getJSONArray("classrooms").getJSONObject(0).getString("name"))
        assertEquals("Aluno Sintético", root.getJSONArray("students").getJSONObject(0).getString("name"))
        assertEquals("revoked", root.getJSONArray("files").getJSONObject(0).getString("accessState"))
        assertFalse(root.has("token"))
        assertFalse(root.has("secret"))
    }

    @Test fun rejectsForeignOrFutureBackupBeforeRestoreCanTouchDatabase() {
        val foreign = JSONObject().put("format", "outro-app").put("version", 1).toString()
        assertThrows(IllegalArgumentException::class.java) { TeacherBackupCodec.validate(foreign) }

        val future = JSONObject(TeacherBackupCodec.encode(snapshot())).put("version", 99).toString()
        assertThrows(IllegalArgumentException::class.java) { TeacherBackupCodec.validate(future) }
    }

    @Test fun rejectsIncompletePayload() {
        val incomplete = JSONObject()
            .put("format", TeacherBackupCodec.FORMAT)
            .put("version", TeacherBackupCodec.VERSION)
            .put("classrooms", org.json.JSONArray())
            .toString()
        assertThrows(IllegalArgumentException::class.java) { TeacherBackupCodec.validate(incomplete) }
    }
}
