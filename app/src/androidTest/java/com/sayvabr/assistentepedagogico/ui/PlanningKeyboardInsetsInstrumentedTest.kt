package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.MainActivity
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Proves actual scrollable editor actions remain reachable after focusing the IME.
 * No screenshots or teacher records; runs only on a disposable emulator.
 */
@RunWith(AndroidJUnit4::class)
class PlanningKeyboardInsetsInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var context: Context
    private var emulator = false
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never modify a real teacher's database", emulator)
        context.deleteDatabase("pedagogico.db")
        TeacherStore(context).use { store ->
            store.saveProfile("Docente para teste de teclado")
            store.createClass("Turma artificial", "Ensino Fundamental", "Matutino")
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
    }

    @After fun cleanup() {
        scenario?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun longLessonEditorCanScrollToSaveWithoutPersistingAnIncompleteDraft() {
        compose.onNodeWithText("Adicionar aula").performScrollTo().performClick()
        // The first text input names a template; the second is the lesson title.
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Aula incompleta fictícia")
        compose.onNodeWithText("Salvar plano de aula").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salvar plano de aula").performClick()
        compose.onNodeWithText("Salvar plano de aula").assertExists()
        TeacherStore(context).use { assertTrue(it.read().lessons.isEmpty()) }
    }

    @Test fun appointmentEditorCanScrollToSaveAfterTypingWithKeyboardOpen() {
        compose.onNodeWithText("Ver compromissos").performScrollTo().performClick()
        compose.onNodeWithText("Novo compromisso").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Reunião sintética")
        compose.onNodeWithText("Salvar compromisso").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salvar compromisso").performClick()
        // Defaults may be accepted or rejected; only reachability/no crash is asserted.
        compose.waitForIdle()
    }
}
