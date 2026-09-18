package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonPlanV6InstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never erase a teacher database on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun input(title: String = "Ciclo da água") = LessonPlanV6.Input(
        title = title,
        subject = "Ciências",
        day = "2026-09-22",
        time = "13:00",
        durationMinutes = 60,
        objective = "Compreender as etapas principais do ciclo da água.",
        specificObjectives = "Identificar evaporação e condensação.",
        content = "Ciclo da água",
        bnccCodes = "EF05CI02",
        justification = "Continuidade do estudo de transformações da água.",
        method = "Experimento, observação e discussão coletiva.",
        opening = "Retomar conhecimentos prévios.",
        openingMinutes = 10,
        development = "Experimento e registro das observações.",
        developmentMinutes = 40,
        closing = "Síntese coletiva.",
        closingMinutes = 10,
        assessment = "Registros do experimento e participação na síntese.",
        adaptations = "Materiais ampliados quando necessário.",
    )

    @Test fun richPlanPersistsAcrossReopenAndKeepsIdentityOnEdit() {
        val first = requireNotNull(store)
        val classroomId = first.createClass("5º A", "Ensino Fundamental", "Matutino")
        val lessonId = first.saveLesson(input(), classroomId)
        first.close()
        store = TeacherStore(context)

        var saved = requireNotNull(store).read().lessons.single()
        assertEquals(lessonId, saved.id)
        assertEquals(60, saved.durationMinutes)
        assertEquals("EF05CI02", saved.bnccCodes)
        assertEquals(10, saved.openingMinutes)
        assertEquals(40, saved.developmentMinutes)
        assertEquals(10, saved.closingMinutes)
        assertTrue(saved.assessment.contains("Registros"))

        requireNotNull(store).updateLesson(classroomId, lessonId, input("Ciclo da água — revisão"))
        requireNotNull(store).close()
        store = TeacherStore(context)
        saved = requireNotNull(store).read().lessons.single()
        assertEquals(lessonId, saved.id)
        assertEquals("Ciclo da água — revisão", saved.title)
        assertEquals("Materiais ampliados quando necessário.", saved.adaptations)
    }
}