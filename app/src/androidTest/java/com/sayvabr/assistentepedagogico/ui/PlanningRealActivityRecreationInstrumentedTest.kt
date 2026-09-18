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

/** MainActivity really closes and reopens its TeacherStore here; never run against a real device. */
@RunWith(AndroidJUnit4::class)
class PlanningRealActivityRecreationInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var context: Context
    private var scenario: ActivityScenario<MainActivity>? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Do not reset real records on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        TeacherStore(context).use { database ->
            database.saveProfile("Docente sintética")
            database.createClass("Turma sintética", "Ensino Fundamental", "Matutino")
        }
    }

    @After fun cleanup() {
        scenario?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun actualActivityRecreationKeepsNewActivityAndDiscardProtection() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Planejamento").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Planejamento").onLast().performClick()
        compose.onNodeWithText("Atividades da turma").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Nova atividade").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nova atividade").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Rascunho após recriação real")

        requireNotNull(scenario).recreate()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Rascunho após recriação real").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Voltar ao planejamento").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações?").assertExists()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Rascunho após recriação real").assertExists()
        TeacherStore(context).use { database ->
            assertTrue(database.listActivities(database.read().classrooms.single().id).isEmpty())
        }
    }
}
