package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic fixtures; destructive database setup is restricted to a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class LessonPlanDuplicationInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never delete a teacher database on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun teardown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun plan() = LessonPlanV6.Input(
        title = "Ciclo da água", subject = "Ciências", day = "2026-09-21", time = "10:00",
        durationMinutes = 60, objective = "Compreender o ciclo da água.",
        specificObjectives = "Identificar evaporação e condensação.", content = "Mudanças de estado",
        bnccCodes = "", justification = "Continuidade da aula anterior", method = "Experimento",
        opening = "Apresentação", openingMinutes = 10,
        development = "Experimento", developmentMinutes = 40,
        closing = "Discussão", closingMinutes = 10,
        assessment = "Registro de observações", adaptations = "Material ampliado",
    )

    @Test fun copiesAllFieldsAndLinkedActivitiesWithoutEditingOriginalAndSurvivesReopen() {
        val first = requireNotNull(store)
        val classId = first.createClass("5º Ano A", "Ensino Fundamental", "Matutino")
        val originalId = first.saveLesson(plan(), classId)
        val activityId = first.saveActivity(LessonActivityV7.Input(
            classId, originalId, "Experimento da água", "Observar e anotar as mudanças de estado.", 25,
        ))
        val original = first.read().lessons.single { it.id == originalId }
        val linkedOriginal = first.listActivities(classId, originalId).single()

        val duplicateId = first.duplicateLesson(classId, originalId, "2026-09-28")
        assertNotEquals(originalId, duplicateId)
        assertEquals(original, first.read().lessons.single { it.id == originalId })
        assertEquals(
            original.copy(id = duplicateId, title = "Ciclo da água (cópia)", date = "2026-09-28"),
            first.read().lessons.single { it.id == duplicateId },
        )
        val linkedCopy = first.listActivities(classId, duplicateId).single()
        assertNotEquals(activityId, linkedCopy.id)
        assertEquals(linkedOriginal.copy(id = linkedCopy.id, lessonId = duplicateId), linkedCopy)
        assertEquals(linkedOriginal, first.listActivities(classId, originalId).single())

        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(2, reopened.read().lessons.size)
        assertEquals(linkedCopy, reopened.listActivities(classId, duplicateId).single())
        assertEquals(linkedOriginal, reopened.listActivities(classId, originalId).single())
    }

    @Test fun refusesCrossClassAndArchivedSourcesWithoutChangingData() {
        val first = requireNotNull(store)
        val classroom = first.createClass("5º Ano A", "Ensino Fundamental", "Matutino")
        val other = first.createClass("5º Ano B", "Ensino Fundamental", "Vespertino")
        val lessonId = first.saveLesson(plan(), classroom)
        val before = first.read().lessons
        try {
            first.duplicateLesson(other, lessonId)
            fail("Cross-class duplication must fail")
        } catch (_: IllegalArgumentException) { assertEquals(before, first.read().lessons) }
        first.setLessonArchived(classroom, lessonId, true)
        try {
            first.duplicateLesson(classroom, lessonId)
            fail("Archived lesson must not be duplicated")
        } catch (_: IllegalArgumentException) { assertEquals(1, first.read().lessons.size) }
    }

    @Test fun rollsBackCopiedLessonWhenActivityInsertFails() {
        val first = requireNotNull(store)
        val classId = first.createClass("5º Ano A", "Ensino Fundamental", "Matutino")
        val lessonId = first.saveLesson(plan(), classId)
        first.saveActivity(LessonActivityV7.Input(
            classId, lessonId, "Experimento da água", "Observar e registrar todas as etapas.", 25,
        ))
        val originalLessons = first.read().lessons
        val originalActivities = first.listActivities(classId)
        first.writableDatabase.execSQL(
            """CREATE TEMP TRIGGER block_copy BEFORE INSERT ON lesson_activities
               WHEN NEW.lesson_id != $lessonId BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END""".trimIndent()
        )
        try {
            first.duplicateLesson(classId, lessonId)
            fail("Synthetic activity insert failure must abort the entire transaction")
        } catch (_: android.database.SQLException) {
            assertEquals(originalLessons, first.read().lessons)
            assertEquals(originalActivities, first.listActivities(classId))
            assertTrue(first.read().lessons.none { it.title.endsWith("(cópia)") })
        }
    }
}
