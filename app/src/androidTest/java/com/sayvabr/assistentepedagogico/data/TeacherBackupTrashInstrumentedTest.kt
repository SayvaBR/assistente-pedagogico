package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Uses a disposable emulator and synthetic SAF references only. */
@RunWith(AndroidJUnit4::class)
class TeacherBackupTrashInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Never delete a real teacher database", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun tearDown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun completeExportIncludesTrashAndRestorePreservesMetadataWithoutPortableSafGrants() {
        val s = requireNotNull(store)
        val folderId = s.createFolder("Materiais fictícios")
        val activeId = s.addFile("Ativo.pdf", "content://synthetic/active")
        val trashedId = s.addFile("Rascunho.pdf", "content://synthetic/trashed")
        s.moveFile(trashedId, folderId)
        s.setFileFavorite(trashedId, true)
        s.trashFile(trashedId)
        assertEquals(listOf(activeId), s.read().files.map { it.id })
        val originalTrash = s.libraryFiles().single { it.id == trashedId }
        assertNotNull(originalTrash.trashedAt)

        val backup = s.exportBackupPayload()
        val json = JSONObject(backup)
        assertEquals(2, json.getJSONArray("files").length())
        assertEquals(2, TeacherBackupRestore.preview(backup).files)
        val exportedTrash = (0 until json.getJSONArray("files").length())
            .map { json.getJSONArray("files").getJSONObject(it) }
            .single { it.getLong("id") == trashedId }
        assertEquals(folderId, exportedTrash.getLong("folderId"))
        assertTrue(exportedTrash.getBoolean("favorite"))
        assertEquals(originalTrash.trashedAt, exportedTrash.getString("trashedAt"))
        assertEquals("revoked", exportedTrash.getString("accessState"))

        // Simulate a destructive restore: all catalog references must return, not just active files.
        s.permanentlyRemoveFileReference(trashedId)
        s.restoreBackupAfterConfirmation(backup, confirmed = true)
        assertEquals(listOf(activeId), s.read().files.map { it.id })
        val trash = s.libraryFiles().single { it.id == trashedId }
        assertEquals(originalTrash.id, trash.id)
        assertEquals(originalTrash.name, trash.name)
        assertEquals(originalTrash.uri, trash.uri)
        assertEquals(originalTrash.folderId, trash.folderId)
        assertTrue(trash.favorite)
        assertEquals(originalTrash.trashedAt, trash.trashedAt)
        assertEquals("revoked", trash.accessState)
        assertEquals("revoked", s.read().files.single().accessState)

        s.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(2, reopened.libraryFiles().size)
        assertEquals(1, reopened.read().files.size)
        reopened.restoreFile(trashedId)
        val restored = reopened.read().files.single { it.id == trashedId }
        assertNull(restored.trashedAt)
        assertEquals(folderId, restored.folderId)
        assertTrue(restored.favorite)
        assertFalse(restored.uri.isBlank())
    }
}
