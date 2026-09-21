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
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic classroom journey: a Home shortcut should save and return to its originating screen. */
@RunWith(AndroidJUnit4::class)
class ClassFlowInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
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

    private fun openApp() {
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
    }

    @Test fun attendanceStartedFromHomeSavesAndReturnsToHome() {
        val database = requireNotNull(store)
        openApp()

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

    @Test fun androidBackAndClassDetailArrowReturnToTheClassListOneStepAtATime() {
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Suas turmas").assertExists()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()
        compose.onNodeWithText("Fazer chamada de hoje").assertExists()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("Suas turmas").assertExists()

        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Suas turmas").assertExists()
    }

    @Test fun classDetailTabsKeepThePrimaryActionAndSeparateClassTasks() {
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()

        compose.onNodeWithText("Fazer chamada de hoje").assertExists()
        compose.onNodeWithText("Acessos rápidos").assertExists()
        compose.onNodeWithText("Alunos").performClick()
        compose.onNodeWithText("Adicionar aluno").assertExists()
        compose.onNodeWithText("Acessos rápidos").assertDoesNotExist()
        compose.onNodeWithText("Fazer chamada de hoje").assertDoesNotExist()

        compose.onNodeWithText("Frequência").performClick()
        compose.onNodeWithText("Fazer chamada de hoje").assertExists()
        compose.onNodeWithText("Histórico de frequência").assertExists()

        compose.onNodeWithText("Registros").performClick()
        compose.onNodeWithText("Novo registro").assertExists()
        compose.onNodeWithText("Histórico de registros").assertExists()
    }

    @Test fun attendanceFromClassDetailReturnsToFrequencyWithTheSavedSummary() {
        val database = requireNotNull(store)
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()
        compose.onNodeWithText("Frequência").performClick()
        compose.onNodeWithText("Fazer chamada de hoje").performClick()

        compose.onNodeWithText("Marcar todos como presentes").performScrollTo().performClick()
        compose.onNodeWithText("Salvar frequência").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            database.read().attendanceSessions.singleOrNull()?.members?.singleOrNull()?.status == "P"
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Última chamada").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Última chamada").assertExists()
        compose.onNodeWithText("1 presente • 0 faltas • 0 pendentes").assertExists()
    }

    @Test fun savedAttendanceHistoryRemainsReachableAfterTheLastStudentIsDeleted() {
        val database = requireNotNull(store)
        val initial = database.read()
        val classroom = initial.classrooms.single()
        val student = initial.students.single()
        val date = java.time.LocalDate.now().toString()
        database.saveAttendance(classroom.id, date, mapOf(student.id to "P"))
        database.deleteStudent(classroom.id, student.id)

        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()
        compose.onNodeWithText("Frequência").performClick()
        compose.onNodeWithText("Histórico de frequência").assertExists().performClick()
        compose.onNodeWithText(java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .performScrollTo().performClick()
        compose.onNodeWithText("Estudante fictício").assertExists()
    }

    @Test fun pastedRosterAddsAllNamesIncludingHomonymsAndReturnsToClassDetail() {
        val database = requireNotNull(store)
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()

        compose.onNodeWithText("Colar lista").performScrollTo().performClick()
        compose.onNodeWithText("Nomes dos alunos").performTextInput("Ana Silva\n\nBruno Souza\nAna Silva")
        compose.onNodeWithText("Adicionar 3 alunos").performScrollTo().performClick()

        compose.waitUntil(15_000) {
            database.read().students.count { it.name == "Ana Silva" } == 2 &&
                database.read().students.count { it.name == "Bruno Souza" } == 1
        }
        val saved = database.read()
        assertEquals(4, saved.students.size)
        assertEquals(2, saved.students.count { it.name == "Ana Silva" })
        assertEquals(1, saved.students.count { it.name == "Bruno Souza" })
        compose.onAllNodesWithText("Ana Silva · cadastro", substring = true).assertCountEquals(2)
        compose.onNodeWithText("Adicionar aluno").assertExists()
    }

    @Test fun largeRosterCanBeSearchedByNameWithoutChangingRecords() {
        val database = requireNotNull(store)
        val classroomId = database.read().classrooms.single().id
        (2..9).forEach { database.addStudent(classroomId, "Estudante $it") }
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turma sintética").performScrollTo().performClick()
        compose.onNodeWithText("Alunos").performClick()

        compose.onNodeWithText("Buscar aluno pelo nome").performScrollTo().performTextInput("Estudante 9")
        compose.onAllNodesWithText("Estudante 9", substring = true).onLast().assertExists()
        compose.onNodeWithText("Estudante 2").assertDoesNotExist()
        assertEquals(9, database.read().students.size)
    }

    @Test fun archivedClassesCanBeSearchedAndRestoredFromTheFilteredList() {
        val database = requireNotNull(store)
        var previousClass = database.read().classrooms.single().id
        repeat(4) { index ->
            database.setClassArchived(previousClass, true)
            previousClass = database.createClass("Turma arquivada ${index + 1}", "Ensino Fundamental", "Matutino")
            database.setClassArchived(previousClass, true)
        }
        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Buscar turma por nome, etapa ou turno").performTextInput("Turma arquivada 3")
        compose.onAllNodesWithText("Turma arquivada 3", substring = true).onLast().performScrollTo().performClick()

        compose.waitUntil(15_000) {
            database.read().classrooms.single { it.name == "Turma arquivada 3" }.archived.not()
        }
        assertEquals(false, database.read().classrooms.single { it.name == "Turma arquivada 3" }.archived)
        compose.onNodeWithText("Turma arquivada 2").assertDoesNotExist()
    }

    @Test fun restoreAtFreeClassLimitIsDisabledWithAnExplanation() {
        val database = requireNotNull(store)
        val archived = database.createClass("Turma arquivada", "Ensino Fundamental", "Matutino")
        database.setClassArchived(archived, true)
        database.createClass("Turma ativa adicional", "Ensino Fundamental", "Vespertino")

        openApp()
        compose.onAllNodesWithText("Turmas").onLast().performClick()
        compose.onNodeWithText("Turmas arquivadas").performScrollTo()
        compose.onNodeWithText("Turma arquivada").performScrollTo().assertHasNoClickAction()
        compose.onNodeWithText("Libere uma vaga ativa para restaurar esta turma.").assertExists()
        assertEquals(true, database.read().classrooms.single { it.id == archived }.archived)
    }
}
