package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.format.DateTimeParseException

/** Destructive test data is permitted only on a disposable emulator, never a physical phone. */
@RunWith(AndroidJUnit4::class)
class AppointmentCrudInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var disposableEmulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        disposableEmulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", ignoreCase = true)
        assumeTrue("Refusing destructive tests outside a disposable Android emulator", disposableEmulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (disposableEmulator) context.deleteDatabase("pedagogico.db")
    }

    private fun reopen(): TeacherStore {
        store?.close()
        store = TeacherStore(context)
        return store!!
    }

    @Test fun editAndDeleteAffectOnlySelectedAppointmentAndPersistAcrossReopen() {
        val db = store!!
        db.addAppointment("Reunião de planejamento", "2026-09-16", "08:00")
        db.addAppointment("Conselho de classe", "2026-09-16", "10:30")
        val originals = db.read().appointments
        val selectedId = originals.first { it.title == "Reunião de planejamento" }.id
        val otherId = originals.first { it.title == "Conselho de classe" }.id

        db.updateAppointment(selectedId, "Reunião reagendada", "2026-09-17", "14:15")
        val edited = reopen().read().appointments
        assertEquals(2, edited.size)
        assertEquals("Reunião reagendada", edited.first { it.id == selectedId }.title)
        assertEquals("2026-09-17", edited.first { it.id == selectedId }.date)
        assertEquals("14:15", edited.first { it.id == selectedId }.time)
        assertEquals("Conselho de classe", edited.first { it.id == otherId }.title)

        store!!.deleteAppointment(selectedId)
        val remaining = reopen().read().appointments
        assertEquals(1, remaining.size)
        assertEquals(otherId, remaining.single().id)
        assertEquals("Conselho de classe", remaining.single().title)
    }

    @Test fun invalidTimeOrUnknownAppointmentCannotCorruptExistingRecords() {
        val db = store!!
        db.addAppointment("Entrevista", "2026-09-16", "09:00")
        val appointment = db.read().appointments.single()
        try {
            db.updateAppointment(appointment.id, "Horário errado", "2026-09-16", "99:99")
            fail("Invalid time must be rejected")
        } catch (_: DateTimeParseException) { /* Database is not modified. */ }
        try {
            db.updateAppointment(-1L, "Sem registro", "2026-09-16", "09:00")
            fail("Missing appointment must be rejected")
        } catch (_: IllegalArgumentException) { /* No row updated. */ }
        try {
            db.deleteAppointment(-1L)
            fail("Missing appointment must not delete anything")
        } catch (_: IllegalArgumentException) { /* No row deleted. */ }
        val intact = reopen().read().appointments
        assertEquals(1, intact.size)
        assertEquals(appointment, intact.single())
        assertTrue(intact.single().title == "Entrevista")
    }
}
