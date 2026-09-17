package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on a disposable emulator: this suite deletes its own test database. */
@RunWith(AndroidJUnit4::class)
class ObservationIsolationInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", ignoreCase = true)
        assumeTrue("Never erase a teacher database on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun creationRejectsStudentFromAnotherClassWithoutSavingAnything() {
        val first = requireNotNull(store)
        val a = first.createClass("Turma A", "Ensino Fundamental", "Matutino")
        val b = first.createClass("Turma B", "Ensino Fundamental", "Vespertino")
        first.addStudent(a, "Alice")
        first.addStudent(b, "Bruno")
        val students = first.read().students
        val aliceId = students.single { it.classroomId == a }.id
        val brunoId = students.single { it.classroomId == b }.id

        try {
            first.addObservation(a, brunoId, "Aprendizagem", "Não pode associar aluno de outra turma.", false)
            fail("Expected rejection for a student belonging to another class")
        } catch (_: IllegalArgumentException) {
            // The database must be untouched by this invalid association.
        }
        assertEquals(0, first.read().observations.size)

        first.addObservation(a, aliceId, "Participação", "Registro de Alice na própria turma.", false)
        first.addObservation(a, null, "Outro", "Registro para a turma inteira.", false)
        first.close()
        store = TeacherStore(context)
        val saved = requireNotNull(store).read().observations
        assertEquals(2, saved.size)
        assertEquals(a, saved[0].classroomId)
        assertNull(saved[0].studentId)
        assertEquals(aliceId, saved[1].studentId)
    }
}
