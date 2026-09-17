package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the proposed v3 -> v4 additive schema works on the Android SQLite
 * implementation used by our instrumented runner before TeacherStore adopts it.
 * Fixtures are deliberately synthetic and the database is private to this test.
 */
@RunWith(AndroidJUnit4::class)
class FilesV4SqliteCompatibilityTest {
    private lateinit var context: Context
    private val databaseName = "files-v4-compat-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun additiveMigrationPreservesLegacyRowsAndForeignKeySetNull() {
        val db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)")
            db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (7,'Documento sintético','content://synthetic/document/7')")

            db.beginTransaction()
            try {
                db.execSQL("CREATE TABLE file_folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, created_at TEXT NOT NULL)")
                db.execSQL("ALTER TABLE saved_files ADD COLUMN folder_id INTEGER REFERENCES file_folders(id) ON DELETE SET NULL")
                db.execSQL("ALTER TABLE saved_files ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1))")
                db.execSQL("ALTER TABLE saved_files ADD COLUMN trashed_at TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE saved_files ADD COLUMN access_state TEXT NOT NULL DEFAULT 'unknown' CHECK(access_state IN ('unknown','available','revoked'))")
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            db.rawQuery("SELECT id,name,uri,folder_id,favorite,trashed_at,access_state FROM saved_files WHERE id=7", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7L, cursor.getLong(0))
                assertEquals("Documento sintético", cursor.getString(1))
                assertEquals("content://synthetic/document/7", cursor.getString(2))
                assertTrue(cursor.isNull(3))
                assertEquals(0, cursor.getInt(4))
                assertTrue(cursor.isNull(5))
                assertEquals("unknown", cursor.getString(6))
            }

            db.execSQL("INSERT INTO file_folders(id,name,created_at) VALUES (3,'Planejamento','2026-09-17T12:00:00Z')")
            db.execSQL("UPDATE saved_files SET folder_id=3 WHERE id=7")
            db.execSQL("DELETE FROM file_folders WHERE id=3")
            db.rawQuery("SELECT folder_id FROM saved_files WHERE id=7", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertNull(if (cursor.isNull(0)) null else cursor.getLong(0))
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun constraintsRejectDuplicateFolderAndInvalidStateWithoutDamagingLegacyRow() {
        val db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        try {
            db.execSQL("CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE, favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1)), access_state TEXT NOT NULL DEFAULT 'unknown' CHECK(access_state IN ('unknown','available','revoked')))")
            db.execSQL("CREATE TABLE file_folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, created_at TEXT NOT NULL)")
            db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (1,'Arquivo sintético','content://synthetic/file/1')")
            db.execSQL("INSERT INTO file_folders(name,created_at) VALUES ('Planos','2026-09-17T12:00:00Z')")

            var duplicateRejected = false
            try { db.execSQL("INSERT INTO file_folders(name,created_at) VALUES ('planos','2026-09-17T12:01:00Z')") } catch (_: Exception) { duplicateRejected = true }
            assertTrue(duplicateRejected)

            var invalidStateRejected = false
            try { db.execSQL("UPDATE saved_files SET access_state='invalid' WHERE id=1") } catch (_: Exception) { invalidStateRejected = true }
            assertTrue(invalidStateRejected)

            db.rawQuery("SELECT name,uri,access_state FROM saved_files WHERE id=1", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Arquivo sintético", cursor.getString(0))
                assertEquals("content://synthetic/file/1", cursor.getString(1))
                assertEquals("unknown", cursor.getString(2))
            }
        } finally {
            db.close()
        }
    }
}
