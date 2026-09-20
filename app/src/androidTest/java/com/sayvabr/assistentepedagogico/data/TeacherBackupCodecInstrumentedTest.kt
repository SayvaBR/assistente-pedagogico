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
        lessons = listOf(Lesson(
            id = 9,
            classroomId = 7,
            title = "Frações",
            subject = "Matemática",
            date = "2026-09-18",
            time = "08:00",
            objective = "Compreender frações",
            content = "Metades",
            method = "Materiais concretos",
            archived = false,
            durationMinutes = 70,
            specificObjectives = "Comparar representações",
            bnccCodes = "EF05MA03, EF05MA04",
            justification = "Continuidade da aula anterior",
            opening = "Retomar conhecimentos prévios",
            openingMinutes = 10,
            development = "Resolver situações com materiais concretos",
            developmentMinutes = 45,
            closing = "Sistematizar estratégias",
            closingMinutes = 15,
            assessment = "Registro das estratégias usadas",
            adaptations = "Material ampliado e apoio visual",
        )),
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
        val session = root.getJSONArray("attendanceSessions").getJSONObject(0)
        assertFalse(session.getBoolean("rosterComplete"))
        assertEquals("Aluno Sintético", session.getJSONArray("members").getJSONObject(0).getString("studentName"))
        assertEquals("revoked", root.getJSONArray("files").getJSONObject(0).getString("accessState"))
        assertFalse(root.has("token"))
        assertFalse(root.has("secret"))
    }

    @Test fun backupPreservesEveryRichLessonV6FieldIncludingBncc() {
        val lesson = TeacherBackupCodec.validate(TeacherBackupCodec.encode(snapshot()))
            .getJSONArray("lessons").getJSONObject(0)

        assertEquals(9L, lesson.getLong("id"))
        assertEquals(7L, lesson.getLong("classroomId"))
        assertEquals(70, lesson.getInt("durationMinutes"))
        assertEquals("Comparar representações", lesson.getString("specificObjectives"))
        assertEquals("EF05MA03, EF05MA04", lesson.getString("bnccCodes"))
        assertEquals("Continuidade da aula anterior", lesson.getString("justification"))
        assertEquals("Retomar conhecimentos prévios", lesson.getString("opening"))
        assertEquals(10, lesson.getInt("openingMinutes"))
        assertEquals("Resolver situações com materiais concretos", lesson.getString("development"))
        assertEquals(45, lesson.getInt("developmentMinutes"))
        assertEquals("Sistematizar estratégias", lesson.getString("closing"))
        assertEquals(15, lesson.getInt("closingMinutes"))
        assertEquals("Registro das estratégias usadas", lesson.getString("assessment"))
        assertEquals("Material ampliado e apoio visual", lesson.getString("adaptations"))
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
