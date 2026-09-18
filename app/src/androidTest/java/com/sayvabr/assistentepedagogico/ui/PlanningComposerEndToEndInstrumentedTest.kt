package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.*
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose navigation + SQLite, with synthetic data on disposable emulators only. */
@RunWith(AndroidJUnit4::class)
class PlanningComposerEndToEndInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false
    private var classroomId = -1L
    private var originalId = -1L

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("This test must never reset a physical device database", emulator)
        context.deleteDatabase("pedagogico.db")
        val database = TeacherStore(context)
        store = database
        database.saveProfile("Docente de teste fictícia")
        classroomId = database.createClass("Turma de teste fictícia", "Ensino Fundamental", "Matutino")
        val lesson = LessonPlanV6.Input(
            title = "Aula fictícia de investigação", subject = "Ciências", day = LocalDate.now().toString(),
            time = "08:00", durationMinutes = 50, objective = "Investigar objetos inventados",
            content = "Transformações fictícias", method = "Investigação guiada",
        )
        val composition = PlanComposition.standard().add(
            PlanBlock("custom_ui_round_trip", PlanBlockKind.CUSTOM, "Anotações fictícias", "Nota sintética original")
        )
        originalId = database.createComposedLesson(classroomId, lesson, composition)
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun editedCustomSectionIsPersistedReopenedAndDuplicatedFromRealUi() {
        val database = requireNotNull(store)
        compose.setContent {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides database) { TeacherApp(database) }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
        compose.onNodeWithText("Abrir e editar plano").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Seção 12 de 12").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasSetTextAction()).onLast().performScrollTo()
            .performTextReplacement("Nota fictícia atualizada pelo editor")
        compose.onNodeWithText("Salvar alterações").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            database.lessonComposition(classroomId, originalId).blocks.last().body ==
                "Nota fictícia atualizada pelo editor"
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Abrir e editar plano").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Abrir e editar plano").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Seção 12 de 12").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasSetTextAction()).onLast()
            .assertTextContains("Nota fictícia atualizada pelo editor")
        compose.onNodeWithText("Duplicar plano e atividades").performScrollTo().performClick()
        compose.onNodeWithText("Duplicar").performClick()
        compose.waitUntil(15_000) { database.read().lessons.size == 2 }
        compose.runOnIdle {
            val lessons = database.read().lessons
            assertEquals(2, lessons.size)
            val copy = lessons.single { it.id != originalId }
            assertEquals(database.lessonComposition(classroomId, originalId),
                database.lessonComposition(classroomId, copy.id))
            assertTrue(lessons.all { it.classroomId == classroomId })
        }
    }
}
