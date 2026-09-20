package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Compose routes, restoration and discard dialogs, without screenshots or real school data. */
@RunWith(AndroidJUnit4::class)
class PlanningNavigationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Planning UI test may reset data only on a disposable emulator", emulator)
        context.deleteDatabase("pedagogico.db")
        val fresh = TeacherStore(context)
        store = fresh
        fresh.saveProfile("Docente fictícia")
        fresh.createClass("Turma sintética", "Ensino Fundamental", "Matutino")
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun openPlanning(restoration: StateRestorationTester? = null) {
        val database = requireNotNull(store)
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides database) {
                    TeacherApp(database)
                }
            }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
        compose.onNodeWithText("Adicionar aula").assertExists()
    }

    private fun activateEditorBack() {
        compose.onNodeWithContentDescription("Voltar")
            .assert(hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick) { onClick -> onClick() }
    }

    private fun enterLessonTitle(value: String) {
        compose.onNode(hasSetTextAction() and hasText("Título da aula"))
            .performScrollTo()
            .performTextInput(value)
        compose.onNodeWithText(value).assertExists()
    }

    @Test fun unsavedLessonBackRequiresConfirmationAndCanKeepEditingOrDiscard() {
        openPlanning()
        compose.onNodeWithText("Adicionar aula").performScrollTo().performClick()
        compose.onNodeWithText("Novo plano de aula").assertExists()
        enterLessonTitle("Plano totalmente fictício")
        activateEditorBack()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Plano totalmente fictício").assertExists()
        activateEditorBack()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Adicionar aula").assertExists()
        assertTrue(requireNotNull(store).read().lessons.isEmpty())
    }

    @Test fun unsavedAppointmentBackCannotSilentlyPersistAndDiscardReturnsToAgenda() {
        openPlanning()
        compose.onNodeWithText("Ver compromissos").performScrollTo().performClick()
        compose.onNodeWithText("Novo compromisso").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Reunião inteiramente fictícia")
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Novo compromisso").assertExists()
        assertTrue(requireNotNull(store).read().appointments.isEmpty())
    }

    @Test fun restoredUnsavedLessonRetainsFieldsAndMustConfirmDiscard() {
        val restoration = StateRestorationTester(compose)
        openPlanning(restoration)
        compose.onNodeWithText("Adicionar aula").performScrollTo().performClick()
        enterLessonTitle("Rascunho após rotação")
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Rascunho após rotação").fetchSemanticsNodes().isNotEmpty() }
        activateEditorBack()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Rascunho após rotação").assertExists()
        assertTrue(requireNotNull(store).read().lessons.isEmpty())
    }

    @Test fun restoredUnsavedAppointmentCannotEscapeWithoutConfirmation() {
        val restoration = StateRestorationTester(compose)
        openPlanning(restoration)
        compose.onNodeWithText("Ver compromissos").performScrollTo().performClick()
        compose.onNodeWithText("Novo compromisso").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Compromisso após rotação")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Novo compromisso").assertExists()
        assertTrue(requireNotNull(store).read().appointments.isEmpty())
    }
}
