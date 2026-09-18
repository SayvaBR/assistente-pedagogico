package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable fixture only: no real files are read, opened or deleted. */
@RunWith(AndroidJUnit4::class)
class FileImportPolicyInstrumentedTest {
    private val databaseName = "test-file-reimport-synthetic.db"
    private lateinit var context: Context
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)")
        FileLibraryV4.migrate(db)
    }

    @After fun tearDown() {
        db.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun reselectTrashedUriRestoresAccessWithoutLosingFavoriteFolderNameOrId() {
        val original = FileImportPolicy.importReference(db, "Plano de Ciências", "content://synthetic/provider/17")
        val folder = FileLibraryV4.createFolder(db, "Aulas")
        FileLibraryV4.moveFile(db, original, folder)
        FileLibraryV4.setFavorite(db, original, true)
        FileLibraryV4.setAccessState(db, original, "revoked")
        FileLibraryV4.trash(db, original)

        val reimported = FileImportPolicy.importReference(db, "Nome do provedor mudou", "content://synthetic/provider/17")
        assertEquals(original, reimported)
        val files = FileLibraryV4.files(db)
        assertEquals(1, files.size)
        val restored = files.single()
        assertNull(restored.trashedAt)
        assertEquals(folder, restored.folderId)
        assertTrue(restored.favorite)
        assertEquals("Plano de Ciências", restored.name)
        assertEquals("available", restored.accessState)

        db.close()
        db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        assertEquals(restored, FileLibraryV4.files(db).single())
    }

    @Test fun duplicateActiveImportKeepsSingleReferenceAndUserChosenName() {
        val original = FileImportPolicy.importReference(db, "Primeiro nome", "content://synthetic/second")
        val repeated = FileImportPolicy.importReference(db, "Segundo nome", "content://synthetic/second")
        assertEquals(original, repeated)
        assertEquals("Primeiro nome", FileLibraryV4.files(db).single().name)
        assertEquals(1, FileLibraryV4.files(db).size)
    }

    @Test fun invalidInputNeverMutatesExistingFiles() {
        FileImportPolicy.importReference(db, "Material", "content://synthetic/existing")
        val before = FileLibraryV4.files(db)
        for ((name, uri) in listOf(
            " " to "content://synthetic/blank",
            "Exemplo" to "file:///storage/emulated/0/private",
            "Exemplo" to "https://example.org/document",
            "Exemplo" to "content://",
            "x".repeat(181) to "content://synthetic/too-long"
        )) {
            try {
                FileImportPolicy.importReference(db, name, uri)
                fail("Invalid name or URI must be rejected")
            } catch (_: IllegalArgumentException) {
                // Explicit rejection is expected; the previous catalog must remain intact.
            }
        }
        assertEquals(before, FileLibraryV4.files(db))
    }

    @Test fun otherDocumentIsIndependentAndPermanentDeleteNeverTouchesOriginalUri() {
        val first = FileImportPolicy.importReference(db, "A", "content://synthetic/A")
        val second = FileImportPolicy.importReference(db, "B", "content://synthetic/B")
        FileLibraryV4.trash(db, first)
        FileLibraryV4.permanentlyRemoveReference(db, first)
        assertEquals(listOf(second), FileLibraryV4.files(db).map { it.id })
        val readded = FileImportPolicy.importReference(db, "A", "content://synthetic/A")
        assertNotEquals(first, readded)
        assertEquals(2, FileLibraryV4.files(db).size)
    }
}
