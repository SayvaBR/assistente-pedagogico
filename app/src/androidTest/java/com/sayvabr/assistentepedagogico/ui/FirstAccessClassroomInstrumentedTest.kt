package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.ClassroomRules
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Covers the real first-access path with synthetic data on a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class FirstAccessClassroomInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("First-access test may reset data only on a disposable emulator", emulator)
        context.deleteDatabase("pedagogico.db")
        context.getSharedPreferences("first_access", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        store?.close()
        if (emulator) {
            context.deleteDatabase("pedagogico.db")
            context.getSharedPreferences("first_access", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun personalizedSetupCreatesAndOpensFirstClassWithUnknownShift() {
        val database = TeacherStore(context)
        store = database
        compose.setContent { ApTheme { FirstAccessFlow(database) } }

        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Preparar meu espaço").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Preparar meu espaço").performClick()
        compose.onNodeWithText("Seu nome").performTextInput("Docente fictícia")
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Ensino Fundamental").performClick()
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Nome da turma").performTextInput("5º A sintética")
        compose.onNodeWithText("Criar meu espaço").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Sua turma foi salva", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        val saved = database.read()
        assertEquals("Docente fictícia", saved.profile?.name)
        assertEquals("5º A sintética", saved.classrooms.single().name)
        assertEquals("Ensino Fundamental", saved.classrooms.single().stage)
        assertEquals(ClassroomRules.unspecifiedShift, saved.classrooms.single().shift)

        compose.onNodeWithText("Entrar no meu espaço").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Olá, Docente!").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Fazer chamada").assertExists()
    }
}
