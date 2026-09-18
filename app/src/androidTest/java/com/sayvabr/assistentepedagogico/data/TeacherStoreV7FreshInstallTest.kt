package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Destructive fixtures run only on a disposable Android emulator, never a teacher device. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreV7FreshInstallTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Only a disposable emulator may delete the synthetic database", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun freshInstallCrudDuplicateAndReopenPreservePlanAndClassIsolation() {
        val first = requireNotNull(store)
        assertEquals(LessonActivityV7.VERSION, first.readableDatabase.version)
        val classroomId = first.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        val otherClassId = first.createClass("Outra turma fictícia", "Ensino Fundamental", "Vespertino")
        first.saveLesson(
            classroomId, "Frações e representações", "Matemática", "2026-09-18", "08:00",
            "Compreender representações de frações.", "Frações", "Atividade com material concreto",
        )
        val lessonId = first.read().lessons.single().id
        val originalId = first.saveActivity(LessonActivityV7.Input(
            classroomId, lessonId, "Representar frações", "Usar peças fictícias para representar frações.", 25,
        ))
        val copyId = first.duplicateActivity(classroomId, originalId)
        assertNotEquals(originalId, copyId)
        first.updateActivity(copyId, LessonActivityV7.Input(
            classroomId, lessonId, "Representar frações — revisão", "Rever as frações com outras peças.", 30,
        ))
        assertEquals("Representar frações", first.listActivities(classroomId, lessonId).single { it.id == originalId }.title)
        assertTrue(first.listActivities(otherClassId).isEmpty())
        first.deleteActivity(classroomId, copyId)
        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(originalId, reopened.listActivities(classroomId, lessonId).single().id)
        assertEquals(lessonId, reopened.read().lessons.single().id)
        assertTrue(reopened.listActivities(otherClassId).isEmpty())
    }
}
