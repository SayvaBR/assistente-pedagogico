package com.sayvabr.assistentepedagogico.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic-only codec validation; does not perform backup I/O or touch a teacher database. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupV5InstrumentedTest {
    private fun snapshot() = TeacherSnapshot(
        profile = TeacherProfile("Docente Fictícia"),
        classrooms = listOf(Classroom(7L, "Turma de Teste", "Ensino Fundamental", "Matutino")),
        students = emptyList(), lessons = emptyList(), attendance = emptyList(),
        observations = emptyList(),
        appointments = listOf(
            Appointment(91L, "Conselho", "2026-09-21", "13:00", "14:30", "Reunião", 7L),
            Appointment(92L, "Compromisso geral", "2026-09-22", "08:00", "09:00", "Pessoal", null),
        ),
        files = emptyList(),
    )

    @Test fun exportPreservesDurationCategoryAndOptionalClassForEachEvent() {
        val root = TeacherBackupCodec.validate(TeacherBackupCodec.encode(snapshot()))
        val events = root.getJSONArray("appointments")
        val classroom = events.getJSONObject(0)
        assertEquals(91L, classroom.getLong("id"))
        assertEquals("13:00", classroom.getString("time"))
        assertEquals("14:30", classroom.getString("endTime"))
        assertEquals("Reunião", classroom.getString("type"))
        assertEquals(7L, classroom.getLong("classroomId"))
        val general = events.getJSONObject(1)
        assertEquals("Pessoal", general.getString("type"))
        assertTrue(general.isNull("classroomId"))
        assertFalse(root.has("token"))
    }

    @Test fun earlierVersionOnePayloadWithoutNewOptionalFieldsRemainsValid() {
        val old = JSONObject(TeacherBackupCodec.encode(snapshot()))
        val firstEvent = old.getJSONArray("appointments").getJSONObject(0)
        firstEvent.remove("endTime")
        firstEvent.remove("type")
        firstEvent.remove("classroomId")
        val validated = TeacherBackupCodec.validate(old.toString())
        assertEquals(TeacherBackupCodec.VERSION, validated.getInt("version"))
        assertFalse(validated.getJSONArray("appointments").getJSONObject(0).has("endTime"))
    }

    @Test fun folderlessLegacyBackupCanPreviewAndRetainUnfiledDocuments() {
        val legacy = JSONObject(TeacherBackupCodec.encode(snapshot().copy(
            files = listOf(SavedFile(12L, "Documento fictício", "content://synthetic/legacy")),
        )))
        legacy.remove("folders")
        val validated = TeacherBackupCodec.validate(legacy.toString())
        assertEquals(0, validated.getJSONArray("folders").length())
        assertEquals(12L, validated.getJSONArray("files").getJSONObject(0).getLong("id"))
        assertEquals(1, TeacherBackupRestore.preview(legacy.toString()).files)
    }

    @Test fun missingFolderCatalogWithLinkedDocumentsIsRejectedBeforeRestore() {
        val corrupted = JSONObject(TeacherBackupCodec.encode(snapshot().copy(
            files = listOf(SavedFile(12L, "Documento fictício", "content://synthetic/legacy", folderId = 88L)),
        )))
        corrupted.remove("folders")
        val failure = assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(corrupted.toString())
        }
        assertTrue(failure.message.orEmpty().contains("pastas"))
    }
}
