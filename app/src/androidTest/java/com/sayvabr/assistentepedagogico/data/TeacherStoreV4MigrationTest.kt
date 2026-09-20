package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Deletes only a synthetic app DB on a disposable emulator; never execute on a physical device. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreV4MigrationTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", true)
        assumeTrue("Never delete a production database on a physical device", emulator)
        context.deleteDatabase("pedagogico.db")
        val db = context.openOrCreateDatabase("pedagogico.db", Context.MODE_PRIVATE, null)
        try {
            listOf(
                "CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL)",
                "CREATE TABLE classrooms (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, stage TEXT NOT NULL, shift TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)",
                "CREATE TABLE students (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, name TEXT NOT NULL)",
                "CREATE TABLE lessons (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, title TEXT NOT NULL, subject TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL, objective TEXT NOT NULL, content TEXT NOT NULL, method TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)",
                "CREATE TABLE attendance (classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE, day TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('P','F')), PRIMARY KEY(student_id,day))",
                "CREATE TABLE observations (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER REFERENCES students(id) ON DELETE SET NULL, kind TEXT NOT NULL, body TEXT NOT NULL, day TEXT NOT NULL, share_approved INTEGER NOT NULL DEFAULT 0)",
                "CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)",
                "CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)",
            ).forEach(db::execSQL)
            db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (13,'Documento A','content://synthetic/a')")
            db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (14,'Documento B','content://synthetic/b')")
            db.execSQL("INSERT INTO appointments(id,title,day,time) VALUES (47,'Reunião anterior','2026-09-21','08:00')")
            db.version = 3
        } finally { db.close() }
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun migrationPreservesLegacyIdentityAllowsOrganizationAndReopensSafely() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        assertEquals(AttendanceV10.VERSION, first.readableDatabase.version)
        assertEquals(listOf(14L, 13L), first.read().files.map { it.id })
        assertEquals("content://synthetic/a", first.read().files.single { it.id == 13L }.uri)
        val formerEvent = first.read().appointments.single()
        assertEquals(47L, formerEvent.id)
        assertEquals("Reunião anterior", formerEvent.title)
        assertEquals("08:00", formerEvent.time)
        assertEquals("08:00", formerEvent.endTime)
        assertEquals("Outro", formerEvent.type)
        assertNull(formerEvent.classroomId)
        val folder = first.createFolder("Materiais")
        first.moveFile(13, folder)
        first.setFileFavorite(13, true)
        first.removeFile(13) // Reversible trash, not a hard DELETE.
        assertEquals(listOf(14L), first.read().files.map { it.id })
        assertEquals(2, first.libraryFiles().size)
        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(AttendanceV10.VERSION, reopened.readableDatabase.version)
        assertEquals(formerEvent, reopened.read().appointments.single())
        val trashed = reopened.libraryFiles().single { it.id == 13L }
        assertEquals(folder, trashed.folderId)
        assertTrue(trashed.favorite)
        assertNotNull(trashed.trashedAt)
        assertEquals("content://synthetic/a", trashed.uri)
        reopened.restoreFile(13)
        assertEquals(listOf(14L, 13L), reopened.read().files.map { it.id })
        assertEquals("Documento A", reopened.read().files.single { it.id == 13L }.name)
        reopened.deleteFolder(folder)
        assertNull(reopened.read().files.single { it.id == 13L }.folderId)
    }

    @Test fun reimportLegacyDocumentRestoresTrashAndAccessWithoutLosingOrganization() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        val folder = first.createFolder("Documentos importantes")
        first.moveFile(13, folder)
        first.setFileFavorite(13, true)
        first.setFileAccessState(13, "revoked")
        first.trashFile(13)
        assertEquals(1, first.read().files.size)

        assertEquals(13L, first.addFile("Nome diferente do seletor", "content://synthetic/a"))
        assertEquals(13L, first.addFile("Importado outra vez", "content://synthetic/a"))
        val restored = first.libraryFiles().single { it.id == 13L }
        assertNull(restored.trashedAt)
        assertEquals(folder, restored.folderId)
        assertTrue(restored.favorite)
        assertEquals("Documento A", restored.name)
        assertEquals("available", restored.accessState)
        assertEquals(2, first.libraryFiles().size)

        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(restored, reopened.libraryFiles().single { it.id == 13L })
        assertEquals(2, reopened.read().files.size)
    }

    @Test fun invalidImportDoesNotEraseOldReferencesAfterUpgrade() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        val before = first.libraryFiles()
        try {
            first.addFile("Documento inseguro", "file:///sdcard/private.txt")
            fail("Must reject non-SAF URI")
        } catch (_: IllegalArgumentException) {
            assertEquals(before, first.libraryFiles())
        }
    }
}
