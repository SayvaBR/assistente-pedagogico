package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic data on an isolated, disposable emulator only. Never deletes a teacher's device data. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupV7ActivitiesInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never erase a production database on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private data class Fixture(val classroom: Long, val other: Long, val lesson: Long, val foreignLesson: Long,
        val linkedActivity: Long, val reusableActivity: Long)

    private fun populate(): Fixture {
        val s = requireNotNull(store)
        s.saveProfile("Professora Fictícia")
        val classroom = s.createClass("Turma Azul", "Ensino Fundamental", "Matutino")
        val other = s.createClass("Turma Branca", "Ensino Fundamental", "Vespertino")
        s.saveLesson(classroom, "Ciclo da água", "Ciências", "2026-09-18", "08:00",
            "Investigar transformações da água", "Água", "Experimentação")
        s.saveLesson(other, "Roda de leitura", "Língua Portuguesa", "2026-09-18", "09:00",
            "Desenvolver leitura compartilhada", "Leitura", "Roda")
        val lesson = s.read().lessons.single { it.classroomId == classroom }.id
        val foreignLesson = s.read().lessons.single { it.classroomId == other }.id
        val linked = s.saveActivity(LessonActivityV7.Input(classroom, lesson, "Experimento guiado",
            "Registrar as transformações observadas no experimento.", 25))
        val reusable = s.saveActivity(LessonActivityV7.Input(classroom, null, "Roda de conversa",
            "Compartilhar hipóteses e organizar uma síntese coletiva.", 15))
        return Fixture(classroom, other, lesson, foreignLesson, linked, reusable)
    }

    @Test fun completeBackupRestoresLinkedAndReusableActivitiesAndStableIdsAfterReopen() {
        val f = populate()
        val s = requireNotNull(store)
        val initial = s.listActivities(f.classroom).toSet()
        val backup = s.exportBackupPayload()
        assertEquals(2, JSONObject(backup).getJSONArray("activities").length())
        assertEquals(2, TeacherBackupRestore.preview(backup).activities)
        assertEquals(2, TeacherBackupRestore.preview(backup).lessons)
        s.updateActivity(f.linkedActivity, LessonActivityV7.Input(f.classroom, f.lesson,
            "Versão descartável", "Este registro será substituído pelo backup.", 35))
        s.saveActivity(LessonActivityV7.Input(f.other, f.foreignLesson, "Outro registro",
            "Este registro também será substituído pelo backup.", 20))
        s.restoreBackupAfterConfirmation(backup, true)
        assertEquals(initial, s.listActivities(f.classroom).toSet())
        assertTrue(s.listActivities(f.other).isEmpty())
        assertEquals(f.lesson, s.listActivities(f.classroom, f.lesson).single().lessonId)
        assertTrue(s.listActivities(f.classroom).single { it.id == f.reusableActivity }.lessonId == null)
        s.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(AttendanceV10.VERSION, reopened.readableDatabase.version)
        assertEquals(initial, reopened.listActivities(f.classroom).toSet())
        assertEquals(2, reopened.read().lessons.size)
    }

    @Test fun restoreMustBeExplicitlyConfirmedAndNeverChangesTheCurrentDatabaseOtherwise() {
        val f = populate()
        val s = requireNotNull(store)
        val backup = s.exportBackupPayload()
        val extraId = s.saveActivity(LessonActivityV7.Input(f.other, f.foreignLesson,
            "Atividade atual que não está no backup", "Este trabalho ainda não foi exportado.", 20))
        val before = s.listActivities(f.classroom) + s.listActivities(f.other)
        assertEquals(2, TeacherBackupRestore.preview(backup).activities)
        assertThrows(IllegalArgumentException::class.java) {
            s.restoreBackupAfterConfirmation(backup, confirmed = false)
        }
        assertEquals(before, s.listActivities(f.classroom) + s.listActivities(f.other))
        assertEquals(3, s.listActivities(f.classroom).size + s.listActivities(f.other).size)
        assertEquals(extraId, s.listActivities(f.other).single().id)
        assertEquals(2, s.read().lessons.size)
    }

    @Test fun foreignLessonAndCorruptActivityArrayAreRejectedBeforeTouchingDatabase() {
        val f = populate()
        val s = requireNotNull(store)
        val before = s.listActivities(f.classroom)
        val backup = s.exportBackupPayload()
        val wrongOwner = JSONObject(backup).apply {
            getJSONArray("activities").getJSONObject(0).put("lessonId", f.foreignLesson)
        }.toString()
        assertThrows(IllegalArgumentException::class.java) { s.restoreBackupAfterConfirmation(wrongOwner, true) }
        assertEquals(before, s.listActivities(f.classroom))
        assertEquals(2, s.read().lessons.size)
        val corruptArray = JSONObject(backup).apply { put("activities", "not-an-array") }.toString()
        assertThrows(IllegalArgumentException::class.java) { s.restoreBackupAfterConfirmation(corruptArray, true) }
        assertEquals(before, s.listActivities(f.classroom))
    }

    @Test fun legacyBackupWithoutActivitiesRemainsReadableAndDoesNotInventActivities() {
        val f = populate()
        val s = requireNotNull(store)
        val legacyPayload = JSONObject(s.exportBackupPayload()).apply { remove("activities") }.toString()
        assertEquals(0, TeacherBackupRestore.preview(legacyPayload).activities)
        s.restoreBackupAfterConfirmation(legacyPayload, true)
        assertTrue(s.listActivities(f.classroom).isEmpty())
        assertEquals(2, s.read().lessons.size)
    }
}
