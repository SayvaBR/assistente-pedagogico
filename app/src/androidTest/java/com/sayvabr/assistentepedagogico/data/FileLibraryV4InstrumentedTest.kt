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

/** Synthetic v3 fixture: never touches an installed teacher database or source documents. */
@RunWith(AndroidJUnit4::class)
class FileLibraryV4InstrumentedTest {
    private val databaseName = "test-file-library-v4.db"
    private lateinit var context: Context
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)")
        db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (17,'Plano sintético','content://synthetic/17')")
        db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (22,'Atividade sintética','content://synthetic/22')")
        db.beginTransaction()
        try {
            FileLibraryV4.migrate(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    @After fun tearDown() {
        db.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun legacyRowsSurviveAndNewDefaultsAreSafe() {
        val files = FileLibraryV4.files(db)
        assertEquals(listOf(22L, 17L), files.map { it.id })
        assertEquals(listOf("content://synthetic/22", "content://synthetic/17"), files.map { it.uri })
        assertTrue(files.all { it.folderId == null && !it.favorite && it.trashedAt == null && it.accessState == "unknown" })
        db.rawQuery("SELECT COUNT(*) FROM saved_files", null).use { assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0)) }
    }

    @Test fun favoritesTrashRestoreFoldersAndAccessPersistAfterDatabaseReopen() {
        val folderId = FileLibraryV4.createFolder(db, "Planos")
        FileLibraryV4.moveFile(db, 17, folderId)
        FileLibraryV4.setFavorite(db, 17, true)
        FileLibraryV4.setAccessState(db, 17, "revoked")
        FileLibraryV4.trash(db, 17)
        assertNotNull(FileLibraryV4.files(db).single { it.id == 17L }.trashedAt)
        FileLibraryV4.restore(db, 17)
        assertNull(FileLibraryV4.files(db).single { it.id == 17L }.trashedAt)
        db.close()
        db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        db.setForeignKeyConstraintsEnabled(true)
        val saved = FileLibraryV4.files(db).single { it.id == 17L }
        assertEquals(folderId, saved.folderId)
        assertTrue(saved.favorite)
        assertEquals("revoked", saved.accessState)
        assertEquals("content://synthetic/17", saved.uri)
        FileLibraryV4.deleteFolder(db, folderId)
        assertNull(FileLibraryV4.files(db).single { it.id == 17L }.folderId)
        assertEquals(2, FileLibraryV4.files(db).size)
    }

    @Test fun duplicateFolderAndInvalidMoveCannotMutateFileOrSourceReference() {
        val first = FileLibraryV4.createFolder(db, "Estudos")
        FileLibraryV4.moveFile(db, 17, first)
        assertFails { FileLibraryV4.createFolder(db, "estudos") }
        assertFails { FileLibraryV4.moveFile(db, 17, first + 9000) }
        assertFails { FileLibraryV4.setAccessState(db, 17, "missing") }
        assertFails { FileLibraryV4.permanentlyRemoveReference(db, 17) }
        val file = FileLibraryV4.files(db).single { it.id == 17L }
        assertEquals(first, file.folderId)
        assertEquals("content://synthetic/17", file.uri)
        assertEquals("unknown", file.accessState)
        assertEquals(2, FileLibraryV4.files(db).size)
    }

    @Test fun onlyPreviouslyTrashedReferenceCanBePermanentlyRemoved() {
        assertFails { FileLibraryV4.permanentlyRemoveReference(db, 17) }
        FileLibraryV4.trash(db, 17)
        FileLibraryV4.permanentlyRemoveReference(db, 17)
        assertEquals(listOf(22L), FileLibraryV4.files(db).map { it.id })
    }

    private inline fun assertFails(action: () -> Unit) {
        var failed = false
        try { action() } catch (_: Exception) { failed = true }
        assertTrue("Invalid operation must fail without deleting metadata", failed)
    }
}
