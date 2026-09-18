package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.LessonActivityV7
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only: verify Activity editor drafts and the parent discard guard survive restoration. */
@RunWith(AndroidJUnit4::class)
class PlanningActivityRestorationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var classroomId = -1L
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Do not delete a real teacher's records", emulator)
        context.deleteDatabase("pedagogico.db")
        val database = TeacherStore(context)
        store = database
        database.saveProfile("Docente totalmente fictícia")
        classroomId = database.createClass("Turma de testes", "Ensino Fundamental", "Matutino")
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun openActivities(restoration: StateRestorationTester) {
        val database = requireNotNull(store)
        restoration.setContent {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides database) { TeacherApp(database) }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
        compose.onNodeWithText("Atividades da turma").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Nova atividade").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun restoredNewActivityRetainsDraftAndRequiresConfirmationToLeave() {
        val restoration = StateRestorationTester(compose)
        openActivities(restoration)
        compose.onNodeWithText("Nova atividade").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Experimento fictício não salvo")
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Experimento fictício não salvo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Voltar ao planejamento").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Experimento fictício não salvo").assertExists()
        compose.onNodeWithText("Voltar ao planejamento").performScrollTo().performClick()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Adicionar aula").assertExists()
        assertTrue(requireNotNull(store).listActivities(classroomId).isEmpty())
    }

    @Test fun restoredEditedActivityDoesNotOverwriteStoredVersionWithoutSaving() {
        val database = requireNotNull(store)
        database.saveActivity(LessonActivityV7.Input(classroomId, null, "Versão original", "Texto fictício", 25))
        val restoration = StateRestorationTester(compose)
        openActivities(restoration)
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Rascunho alterado")
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Rascunho alterado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Voltar ao planejamento").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Descartar").performClick()
        assertEquals("Versão original", database.listActivities(classroomId).single().title)
    }
}
