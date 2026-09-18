package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic records on a disposable emulator only. Never delete a teacher's database on a device. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupRestoreInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Do not erase real records on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun plan(title: String) = LessonPlanV6.Input(
        title = title, subject = "Ciências", day = "2026-09-22", time = "13:00",
        durationMinutes = 60, objective = "Conhecer o ciclo da água na natureza.",
        specificObjectives = "Diferenciar condensação e evaporação.", content = "Ciclo da água",
        bnccCodes = "EF05CI02", justification = "Continuidade da pesquisa anterior.",
        method = "Investigação coletiva", opening = "Perguntas iniciais", openingMinutes = 10,
        development = "Experimento", developmentMinutes = 40, closing = "Síntese", closingMinutes = 10,
        assessment = "Registro em caderno", adaptations = "Material ampliado",
    )

    private fun populate(): String {
        val s = requireNotNull(store)
        s.saveProfile("Professora Fictícia")
        val classId = s.createClass("5º Ano A", "Ensino Fundamental", "Matutino")
        s.addStudent(classId, "Aluna Fictícia")
        val studentId = s.read().students.single().id
        s.saveAttendance(classId, "2026-09-22", mapOf(studentId to "P"))
        s.addObservation(classId, studentId, "Aprendizagem", "Registro inteiramente fictício.", false)
        s.saveLesson(plan("Ciclo da água"), classId)
        s.addAppointment(AppointmentV5.Input("Reunião pedagógica", "2026-09-22", "15:00", "16:00", "Reunião", classId))
        val folder = s.createFolder("Planos")
        val file = s.addFile("Atividade.pdf", "content://synthetic/atividade")
        s.moveFile(file, folder)
        s.setFileFavorite(file, true)
        return TeacherBackupCodec.encode(s.read())
    }

    @Test fun confirmedRestorePreservesRichPlansAcrossDatabaseReopenAndRevokesSafGrant() {
        val backup = populate()
        val s = requireNotNull(store)
        val preview = TeacherBackupRestore.preview(backup)
        assertEquals(1, preview.classrooms)
        assertEquals(1, preview.students)
        assertEquals(1, preview.lessons)
        assertEquals(1, preview.appointments)
        assertEquals(1, preview.files)
        val original = s.read().lessons.single()
        s.saveProfile("Alteração temporária")
        s.updateLesson(original.classroomId, original.id, plan("Título alterado"))
        val before = s.read()

        assertThrows(IllegalArgumentException::class.java) { s.restoreBackupAfterConfirmation(backup, false) }
        assertEquals(before, s.read())
        s.restoreBackupAfterConfirmation(backup, true)
        val restored = s.read()
        assertEquals("Professora Fictícia", restored.profile?.name)
        assertEquals(original, restored.lessons.single())
        assertEquals("EF05CI02", restored.lessons.single().bnccCodes)
        assertEquals("Material ampliado", restored.lessons.single().adaptations)
        assertEquals(1, restored.attendance.size)
        assertEquals(1, restored.observations.size)
        assertEquals(1, restored.appointments.size)
        assertEquals("revoked", restored.files.single().accessState)
        assertEquals(restored.folders.single().id, restored.files.single().folderId)
        assertTrue(restored.files.single().favorite)
        assertNotEquals(before.lessons.single().title, restored.lessons.single().title)

        s.close()
        store = TeacherStore(context)
        assertEquals(restored, requireNotNull(store).read())
    }

    @Test fun malformedRelationsAndSqlFailuresNeverDeleteExistingRecords() {
        val backup = populate()
        val s = requireNotNull(store)
        val before = s.read()
        val badReference = JSONObject(backup).apply {
            getJSONArray("lessons").getJSONObject(0).put("classroomId", 999999)
        }.toString()
        assertThrows(IllegalArgumentException::class.java) { s.restoreBackupAfterConfirmation(badReference, true) }
        assertEquals(before, s.read())

        // Duplicate attendance keys are valid JSON but violate SQLite's composite PRIMARY KEY.
        // This fails after DELETE and INSERT have started: the transaction must restore every old row.
        val badConstraint = JSONObject(backup).apply {
            val entries = getJSONArray("attendance")
            entries.put(JSONObject(entries.getJSONObject(0).toString()))
        }.toString()
        assertThrows(Exception::class.java) { s.restoreBackupAfterConfirmation(badConstraint, true) }
        assertEquals(before, s.read())
        s.close()
        store = TeacherStore(context)
        assertEquals(before, requireNotNull(store).read())
    }
}
