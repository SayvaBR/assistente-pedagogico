package com.sayvabr.assistentepedagogico.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.LessonActivityV7
import com.sayvabr.assistentepedagogico.data.TeacherBackupRestore
import com.sayvabr.assistentepedagogico.data.TeacherStore
import com.sayvabr.assistentepedagogico.data.exportBackupPayload
import com.sayvabr.assistentepedagogico.data.restoreBackupAfterConfirmation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real ContentResolver streams (not a mocked URI); chooser interaction remains a separate gate. */
@RunWith(AndroidJUnit4::class)
class PlanningBackupDocumentIoInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var document: Uri? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never reset real school data or create test downloads on a physical device", emulator)
        assumeTrue("Downloads content provider test requires Android 10+", Build.VERSION.SDK_INT >= 29)
        context.deleteDatabase("pedagogico.db")
        val database = TeacherStore(context)
        store = database
        database.saveProfile("Professora completamente fictícia")
        val classroomId = database.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        database.saveActivity(LessonActivityV7.Input(classroomId, null, "Pesquisa simulada", "Instruções inteiramente sintéticas", 25))
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "ap-backup-test-${System.nanoTime()}.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
        }
        document = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Emulator Downloads provider did not create test document")
    }

    @After fun cleanup() {
        document?.let { context.contentResolver.delete(it, null, null) }
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun actualAndroidDocumentRoundTripPreservesUnicodeAndRequiresRestoreConfirmation() {
        val database = requireNotNull(store)
        val uri = requireNotNull(document)
        val original = database.exportBackupPayload()
        PlanningBackupDocumentIo.write(context.contentResolver, uri, original)
        val imported = PlanningBackupDocumentIo.read(context.contentResolver, uri)
        assertEquals(original, imported)
        assertEquals(1, TeacherBackupRestore.preview(imported).activities)
        database.saveProfile("Alteração temporária sintética")
        assertThrows(IllegalArgumentException::class.java) {
            database.restoreBackupAfterConfirmation(imported, confirmed = false)
        }
        assertEquals("Alteração temporária sintética", database.read().profile?.name)
        database.restoreBackupAfterConfirmation(imported, confirmed = true)
        assertEquals("Professora completamente fictícia", database.read().profile?.name)
        assertEquals(1, database.listActivities(database.read().classrooms.single().id).size)
    }

    @Test fun oversizedDocumentIsRejectedWithoutChangingDatabase() {
        val database = requireNotNull(store)
        val before = database.read()
        val uri = requireNotNull(document)
        context.contentResolver.openOutputStream(uri, "wt")!!.use { output ->
            output.write(ByteArray(PlanningBackupDocumentIo.MAX_BYTES + 1) { 'x'.code.toByte() })
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlanningBackupDocumentIo.read(context.contentResolver, uri)
        }
        assertEquals(before, database.read())
        assertThrows(IllegalArgumentException::class.java) {
            PlanningBackupDocumentIo.write(context.contentResolver, uri, "x".repeat(PlanningBackupDocumentIo.MAX_BYTES + 1))
        }
        assertEquals(before, database.read())
    }
}
