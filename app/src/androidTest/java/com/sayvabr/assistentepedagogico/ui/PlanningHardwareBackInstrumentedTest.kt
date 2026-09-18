package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

/** Tests Activity's Android Back dispatcher without depending on Espresso window focus. */
@RunWith(AndroidJUnit4::class)
class PlanningHardwareBackInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never reset a real teacher's database", emulator)
        context.deleteDatabase("pedagogico.db")
        val database = TeacherStore(context)
        store = database
        database.saveProfile("Professora sintética")
        database.createClass("Turma de teste", "Ensino Fundamental", "Matutino")
        compose.setContent {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides database) { TeacherApp(database) }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun dispatchBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test fun activityBackFromDirtyLessonRequiresDiscardAndKeepsRecordUnsaved() {
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
        compose.onNodeWithText("Adicionar aula").performScrollTo().performClick()
        // The first editable field names a template; section titles use a separate dialog.
        // The second editable field is the actual lesson title.
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Rascunho físico sintético")
        dispatchBack()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Rascunho físico sintético").assertExists()
        dispatchBack()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Adicionar aula").assertExists()
        assertTrue(requireNotNull(store).read().lessons.isEmpty())
    }

    @Test fun activityBackAtHomeAsksBeforeExiting() {
        dispatchBack()
        compose.onNodeWithText("Sair do Assistente Pedagógico?").assertExists()
        compose.onNodeWithText("Continuar no app").performClick()
        compose.onNodeWithText("Que bom ter você aqui hoje", substring = true).assertExists()
    }
}
