package com.sayvabr.assistentepedagogico.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Pure synthetic JSON; never opens or deletes a device database. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupLegacyLateDayInstrumentedTest {
    private fun backup(lesson: JSONObject): JSONObject = JSONObject().apply {
        put("format", TeacherBackupCodec.FORMAT)
        put("version", TeacherBackupCodec.VERSION)
        put("profile", JSONObject.NULL)
        put("classrooms", JSONArray().put(JSONObject().put("id", 1).put("name", "Turma fictícia")
            .put("stage", "Ensino Fundamental").put("shift", "Matutino")))
        put("students", JSONArray())
        put("lessons", JSONArray().put(lesson))
        put("attendance", JSONArray())
        put("observations", JSONArray())
        put("appointments", JSONArray())
        put("files", JSONArray())
        put("folders", JSONArray())
    }

    private fun migratedLesson() = JSONObject()
        .put("id", 3).put("classroomId", 1).put("title", "Plano antigo fictício")
        .put("subject", "Ciências").put("date", "2026-09-22").put("time", "23:30")
        .put("objective", "Objetivo anterior").put("content", "Conteúdo anterior")
        .put("method", "Metodologia anterior")
        // v5 -> v6 populates duration even though the real duration was never recorded.
        .put("durationMinutes", 50).put("specificObjectives", "").put("bnccCodes", "")
        .put("justification", "").put("opening", "").put("openingMinutes", 0)
        .put("development", "").put("developmentMinutes", 0)
        .put("closing", "").put("closingMinutes", 0)
        .put("assessment", "").put("adaptations", "")

    @Test fun exportedMigratedPlanWithDefaultDurationCanStillBeRecovered() {
        assertEquals(1, TeacherBackupRestore.preview(backup(migratedLesson()).toString()).lessons)
    }

    @Test fun enrichedPlanCannotCrossMidnight() {
        val lesson = migratedLesson().put("opening", "Abertura detalhada").put("openingMinutes", 10)
        assertThrows(IllegalArgumentException::class.java) {
            TeacherBackupRestore.preview(backup(lesson).toString())
        }
    }
}
