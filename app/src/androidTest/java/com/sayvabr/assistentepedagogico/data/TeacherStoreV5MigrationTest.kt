package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only; the test database is synthetic and isolated from teacher records. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreV5MigrationTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", true)
        assumeTrue("Do not delete a user database on a physical device", emulator)
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
            FileLibraryV4.migrate(db)
            db.execSQL("INSERT INTO classrooms(id,name,stage,shift) VALUES (5,'Turma histórica','Ensino Fundamental','Matutino')")
            db.execSQL("INSERT INTO appointments(id,title,day,time) VALUES (41,'Reunião legado','2026-09-21','08:00')")
            db.execSQL("INSERT INTO saved_files(id,name,uri,favorite,access_state) VALUES (7,'Material anterior','content://synthetic/file',1,'available')")
            db.version = 4
        } finally { db.close() }
    }

    @After fun teardown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun upgradePreservesIdsMetadataAndNewEventPersistsAfterReopen() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        assertEquals(LessonPlanV6.VERSION, first.readableDatabase.version)
        val legacy = first.read().appointments.single()
        assertEquals(41L, legacy.id)
        assertEquals("Reunião legado", legacy.title)
        assertEquals("08:00", legacy.time)
        assertEquals("08:00", legacy.endTime)
        assertEquals("Outro", legacy.type)
        assertNull(legacy.classroomId)
        assertEquals(7L, first.read().files.single().id)
        assertTrue(first.read().files.single().favorite)

        val createdId = first.addAppointment(AppointmentV5.Input(
            "Reunião atual", "2026-09-21", "10:00", "11:00", "Reunião", 5L,
        ))
        val created = first.read().appointments.single { it.id == createdId }
        assertEquals("11:00", created.endTime)
        assertEquals("Reunião", created.type)
        assertEquals(5L, created.classroomId)

        first.close()
        store = TeacherStore(context)
        val second = requireNotNull(store)
        assertEquals(LessonPlanV6.VERSION, second.readableDatabase.version)
        assertEquals(legacy, second.read().appointments.single { it.id == 41L })
        assertEquals(created, second.read().appointments.single { it.id == createdId })
        assertEquals("content://synthetic/file", second.read().files.single().uri)
    }
}
