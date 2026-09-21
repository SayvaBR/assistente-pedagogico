package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic classroom journey: a Home shortcut should save and return to its originating screen. */
@RunWith(AndroidJUnit4::class)
class ClassFlowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Class UI test may reset data only on a disposable emulator", emulator)
        context.deleteDatabase("pedagogico.db")
        val fresh = TeacherStore(context)
        store = fresh
        fresh.saveProfile("Docente fictícia")
        val classroomId = fresh.createClass("Turma sintética", "Ensino Fundamental", "Matutino")
        fresh.addStudent(classroomId, "Estudante fictício")
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun attendanceStartedFromHomeSavesAndReturnsToHome() {
        val database = requireNotNull(store)
        compose.setContent {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides database) {
                    TeacherApp(database)
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Olá, Docente!").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Fazer chamada").performScrollTo().performClick()
        compose.onNodeWithText("Frequência").assertExists()
        compose.onNodeWithText("Marcar todos como presentes").performScrollTo().performClick()
        compose.onNodeWithText("Salvar frequência").performScrollTo().performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Olá, Docente!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Olá, Docente!").assertExists()
        assertEquals("P", database.read().attendanceSessions.single().members.single().status)
    }
}
