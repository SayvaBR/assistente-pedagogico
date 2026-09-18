package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator and synthetic document URI only; never touch a physical teacher database. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupLegacyFolderRestoreInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never delete production records on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun restoresOldBackupWithoutFoldersAndKeepsDocumentReference() {
        val original = requireNotNull(store)
        val classroomId = original.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        val fileId = original.addFile("Material fictício", "content://synthetic/lesson-material")
        val oldPayload = JSONObject(original.exportBackupPayload()).apply { remove("folders") }.toString()

        // Preview and confirmation use the exact same validation path as the real restore.
        assertEquals(1, TeacherBackupRestore.preview(oldPayload).files)
        original.restoreBackupAfterConfirmation(oldPayload, confirmed = true)
        original.close()
        store = TeacherStore(context)
        val restored = requireNotNull(store)
        assertEquals(classroomId, restored.read().classrooms.single().id)
        assertEquals(fileId, restored.read().files.single().id)
        assertEquals("content://synthetic/lesson-material", restored.read().files.single().uri)
        assertEquals("revoked", restored.read().files.single().accessState)
        assertTrue(restored.fileFolders().isEmpty())
    }

    @Test fun invalidMissingFolderCatalogLeavesExistingDatabaseUntouched() {
        val current = requireNotNull(store)
        current.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        current.addFile("Material fictício", "content://synthetic/lesson-material")
        val before = current.read()
        val damaged = JSONObject(current.exportBackupPayload())
        damaged.getJSONArray("files").getJSONObject(0).put("folderId", 200)
        damaged.remove("folders")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            current.restoreBackupAfterConfirmation(damaged.toString(), confirmed = true)
        }
        assertTrue(exception.message.orEmpty().contains("pastas"))
        assertEquals(before, current.read())
    }
}
