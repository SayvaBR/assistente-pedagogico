package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic v5 fixture on a disposable emulator only. Never erase a teacher database on a device. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreV6MigrationTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", true)
        assumeTrue("Never delete a real teacher database", emulator)
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
            AppointmentV5.migrate(db)
            db.execSQL("INSERT INTO profile(id,name) VALUES (1,'Docente Fictícia')")
            db.execSQL("INSERT INTO classrooms(id,name,stage,shift) VALUES (31,'5º Ano A','Ensino Fundamental','Matutino')")
            db.execSQL("INSERT INTO students(id,classroom_id,name) VALUES (32,31,'Aluna Fictícia')")
            db.execSQL("INSERT INTO lessons(id,classroom_id,title,subject,day,time,objective,content,method,archived) VALUES (41,31,'Ciclo da água','Ciências','2026-09-22','13:00','Observar a água','Mudanças de estado','Experimento',1)")
            db.execSQL("INSERT INTO attendance(classroom_id,student_id,day,status) VALUES (31,32,'2026-09-22','P')")
            db.execSQL("INSERT INTO observations(id,classroom_id,student_id,kind,body,day,share_approved) VALUES (44,31,32,'Aprendizagem','Registro fictício','2026-09-22',1)")
            db.execSQL("INSERT INTO appointments(id,title,day,time,end_time,type,classroom_id) VALUES (42,'Reunião','2026-09-22','14:00','15:00','Reunião',31)")
            db.execSQL("INSERT INTO saved_files(id,name,uri,favorite,access_state) VALUES (43,'Atividade','content://synthetic/activity',1,'available')")
            db.version = AppointmentV5.VERSION
        } finally { db.close() }
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun v5ToV6KeepsAllRecordsAndAddsSafeLessonDefaultsAcrossReopen() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        assertEquals(LessonActivityV7.VERSION, first.readableDatabase.version)
        val snapshot = first.read()
        assertEquals("Docente Fictícia", snapshot.profile?.name)
        assertEquals(31L, snapshot.classrooms.single().id)
        assertEquals(32L, snapshot.students.single().id)
        val original = snapshot.lessons.single()
        assertEquals(41L, original.id)
        assertEquals(31L, original.classroomId)
        assertEquals("Ciclo da água", original.title)
        assertEquals("Ciências", original.subject)
        assertEquals("2026-09-22", original.date)
        assertEquals("13:00", original.time)
        assertEquals("Observar a água", original.objective)
        assertEquals("Mudanças de estado", original.content)
        assertEquals("Experimento", original.method)
        assertTrue(original.archived)
        assertEquals(50, original.durationMinutes)
        assertEquals("", original.bnccCodes)
        assertEquals("", original.opening)
        assertEquals(0, original.openingMinutes)
        assertEquals(0, original.developmentMinutes)
        assertEquals(0, original.closingMinutes)
        assertEquals("", original.assessment)
        assertEquals("", original.adaptations)
        assertEquals(32L, snapshot.attendance.single().studentId)
        assertEquals("P", snapshot.attendance.single().status)
        assertEquals(44L, snapshot.observations.single().id)
        assertTrue(snapshot.observations.single().shareApproved)
        assertEquals(42L, snapshot.appointments.single().id)
        assertEquals("15:00", snapshot.appointments.single().endTime)
        assertEquals("Reunião", snapshot.appointments.single().type)
        assertEquals(31L, snapshot.appointments.single().classroomId)
        assertEquals(43L, snapshot.files.single().id)
        assertTrue(snapshot.files.single().favorite)
        assertFalse(snapshot.folders.any())

        // Running the migration twice is safe, including after SQLiteOpenHelper upgrades the version.
        LessonPlanV6.migrate(first.writableDatabase)
        assertEquals(snapshot, first.read())
        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(LessonActivityV7.VERSION, reopened.readableDatabase.version)
        assertEquals(snapshot, reopened.read())
    }
}
