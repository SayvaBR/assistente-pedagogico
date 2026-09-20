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

/** Synthetic records only. The fixture must never delete a teacher's physical-device database. */
@RunWith(AndroidJUnit4::class)
class PlanLayoutV9InstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Only disposable emulators can create synthetic database fixtures", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun sample() = LessonPlanV6.Input(
        title = "Aula fictícia de Ciências", subject = "Ciências", day = "2026-09-23", time = "08:00",
        durationMinutes = 50, objective = "Investigar objetos fictícios", content = "Mudanças de estado",
        method = "Investigação guiada", opening = "Perguntas iniciais", openingMinutes = 10,
        development = "Exploração", developmentMinutes = 30, closing = "Síntese", closingMinutes = 10,
    )

    private fun customLayout() = PlanComposition.standard()
        .add(PlanBlock("teacher_block_a", PlanBlockKind.CUSTOM, "Registro livre", "Minha nota fictícia"))
        .add(PlanBlock("teacher_block_b", PlanBlockKind.CUSTOM, "Material da escola", "Texto da escola fictícia"))
        .move("teacher_block_b", -1)

    @Test fun freshInstallStoresIndependentLayoutAndTemplateAcrossReopenAndDuplicate() {
        val s = requireNotNull(store)
        assertEquals(AttendanceV10.VERSION, s.readableDatabase.version)
        val classroom = s.createClass("Turma sintética", "Ensino Fundamental", "Matutino")
        val layout = customLayout()
        val lesson = s.createComposedLesson(classroom, sample(), layout)
        val template = layout.saveAsTemplate("Modelo da escola fictícia")
        val templateId = s.createPlanTemplate(template)
        assertEquals(layout, s.lessonComposition(classroom, lesson))
        assertEquals("", s.planTemplates().single().second.blocks.last().body)
        val copy = s.duplicateLesson(classroom, lesson)
        assertEquals(layout, s.lessonComposition(classroom, copy))
        val altered = layout.update(layout.blocks.last().copy(body = "Outra nota fictícia"))
        s.updateComposedLesson(classroom, copy, sample().copy(title = "Cópia editada fictícia"), altered)
        assertEquals(layout, s.lessonComposition(classroom, lesson))
        assertEquals(altered, s.lessonComposition(classroom, copy))
        s.deletePlanTemplate(templateId)
        assertEquals(layout, s.lessonComposition(classroom, lesson))
        s.close()
        store = TeacherStore(context)
        assertEquals(layout, requireNotNull(store).lessonComposition(classroom, lesson))
        assertEquals(altered, requireNotNull(store).lessonComposition(classroom, copy))
    }

    @Test fun backupRestoresLayoutsAndTemplatesAndRejectsMalformedDataWithoutDeletion() {
        val s = requireNotNull(store)
        val classroom = s.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        val layout = customLayout()
        val lessonId = s.createComposedLesson(classroom, sample(), layout)
        s.createPlanTemplate(layout.saveAsTemplate("Modelo da escola"))
        val payload = s.exportBackupPayload()
        val saved = JSONObject(payload)
        assertEquals(1, saved.getJSONArray("lessonLayouts").length())
        assertEquals(1, saved.getJSONArray("planTemplates").length())
        val corrupt = JSONObject(payload)
        corrupt.getJSONArray("lessonLayouts").getJSONObject(0).put("lessonId", 99999L)
        assertThrows(IllegalArgumentException::class.java) {
            s.restoreBackupAfterConfirmation(corrupt.toString(), true)
        }
        assertEquals(layout, s.lessonComposition(classroom, lessonId))
        assertEquals(1, s.planTemplates().size)
        s.restoreBackupAfterConfirmation(payload, true)
        assertEquals(layout, s.lessonComposition(classroom, lessonId))
        assertEquals("Modelo da escola", s.planTemplates().single().second.name)
        val legacy = JSONObject(payload).apply { remove("lessonLayouts"); remove("planTemplates") }
        s.restoreBackupAfterConfirmation(legacy.toString(), true)
        assertEquals(PlanComposition.standard(), s.lessonComposition(classroom, lessonId))
        assertTrue(s.planTemplates().isEmpty())
    }

    @Test fun oldVersionEightGetsAdditiveTablesAndLegacyLessonDefaultsToStandard() {
        val s = requireNotNull(store)
        val classroom = s.createClass("Turma antiga", "Ensino Fundamental", "Matutino")
        val lessonId = s.saveLesson(sample(), classroom)
        s.writableDatabase.execSQL("DROP TABLE lesson_layouts")
        s.writableDatabase.execSQL("DROP TABLE plan_templates")
        s.writableDatabase.version = LessonStatusV8.VERSION
        s.close()
        store = TeacherStore(context)
        val upgraded = requireNotNull(store)
        assertEquals(AttendanceV10.VERSION, upgraded.readableDatabase.version)
        assertEquals(lessonId, upgraded.read().lessons.single().id)
        assertEquals(PlanComposition.standard(), upgraded.lessonComposition(classroom, lessonId))
        assertTrue(upgraded.planTemplates().isEmpty())
    }
}
