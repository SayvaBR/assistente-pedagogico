package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.MoreSettingsStore
import com.sayvabr.assistentepedagogico.data.TeacherStore
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue

@RunWith(AndroidJUnit4::class)
class MoreFlowInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var context: Context
    private lateinit var store: TeacherStore
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("More UI test may reset data only on a disposable emulator", emulator)
        context.deleteDatabase("pedagogico.db")
        context.getSharedPreferences(MoreSettingsStore.FILE, Context.MODE_PRIVATE).edit().clear().commit()
        store = TeacherStore(context)
        store.saveProfile("Docente fictícia")
        val classroom = store.createClass("Turma sintética", "Ensino Fundamental", "Matutino")
        store.addStudent(classroom, "Estudante fictício")
    }

    @After fun cleanup() {
        store.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
        context.getSharedPreferences(MoreSettingsStore.FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun openApp() {
        compose.setContent {
            ApTheme {
                CompositionLocalProvider(LocalTeacherStore provides store) { TeacherApp(store) }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Olá, Docente!").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun moreIndexExposesSettingsDestinationsAndEachReturnsToIndex() {
        openApp()
        compose.onAllNodesWithText("Mais").onLast().performClick()
        listOf("Conta e perfil", "Notificações", "Turmas", "Aparência", "Idioma",
            "Privacidade e segurança", "Sincronização e backup", "Sobre o app", "Sair da conta")
            .forEach { compose.onNodeWithText(it).assertExists() }

        compose.onNodeWithText("Notificações").performScrollTo().performClick()
        compose.onNodeWithText("Alertas, lembretes e comunicados").assertExists()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Aparência").performScrollTo().performClick()
        compose.onNodeWithText("Tema").assertExists()
        compose.onNodeWithText("Escuro").performClick()
        compose.onNodeWithText("Salvar aparência").performClick()
        compose.onNodeWithText("Aparência").assertExists()
        assertEquals("Escuro", MoreSettingsStore.read(context).theme)

        compose.onNodeWithText("Idioma").performScrollTo().performClick()
        compose.onNodeWithText("Português (Brasil)").assertExists()
        compose.onNodeWithText("Salvar idioma").performClick()
        compose.onNodeWithText("Idioma").assertExists()

        compose.onNodeWithText("Sobre o app").performScrollTo().performClick()
        compose.onNodeWithText("Versão 0.0.1").assertExists()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Sincronização e backup").performScrollTo().performClick()
        compose.onNodeWithText("Exportar backup").assertExists()
        compose.onNodeWithContentDescription("Voltar").performClick()
    }

    @Test fun privacyDeletionCancelKeepsLocalData() {
        openApp()
        compose.onAllNodesWithText("Mais").onLast().performClick()
        compose.onNodeWithText("Privacidade e segurança").performScrollTo().performClick()
        compose.onNodeWithText("Excluir tudo").performClick()
        compose.onNodeWithText("Esta ação remove os dados locais e não pode ser desfeita.").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Conta e perfil").assertExists()
        assertEquals("Docente fictícia", store.read().profile?.name)
    }
}
