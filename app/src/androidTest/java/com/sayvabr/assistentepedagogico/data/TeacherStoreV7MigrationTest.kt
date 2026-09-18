package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic legacy database only. Never run destructive database fixtures on physical devices. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreV7MigrationTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator: Boolean = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Only a disposable emulator may create/delete the synthetic legacy DB", emulator)
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
            LessonPlanV6.migrate(db)
            db.execSQL("INSERT INTO classrooms(id,name,stage,shift) VALUES (7,'Turma Azul','Ensino Fundamental','Matutino'),(8,'Turma Branca','Ensino Fundamental','Vespertino')")
            db.execSQL("INSERT INTO lessons(id,classroom_id,title,subject,day,time,objective,content,method) VALUES (17,7,'Plano histórico','Ciências','2026-09-17','08:00','Observar transformações','Água','Experimento'),(18,8,'Outro plano','Língua Portuguesa','2026-09-17','09:00','Ler e compreender','Leitura','Roda')")
            db.execSQL("INSERT INTO appointments(id,title,day,time,end_time,type,classroom_id) VALUES (22,'Reunião','2026-09-17','10:00','11:00','Reunião',7)")
            db.execSQL("INSERT INTO saved_files(id,name,uri) VALUES (31,'Material antigo','content://synthetic/old')")
            db.version = LessonPlanV6.VERSION
        } finally { db.close() }
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    @Test fun v6UpgradeKeepsExistingDataAndActivitiesPersistAfterReopen() {
        store = TeacherStore(context)
        val first = requireNotNull(store)
        assertEquals(LessonActivityV7.VERSION, first.readableDatabase.version)
        assertEquals("Plano histórico", first.read().lessons.single { it.id == 17L }.title)
        assertEquals("10:00", first.read().appointments.single().time)
        assertEquals(31L, first.read().files.single().id)
        assertTrue(first.listActivities(7).isEmpty())

        val id = first.saveActivity(LessonActivityV7.Input(7, 17, "Experimento com água", "Observar e registrar as mudanças da água.", 25))
        assertEquals(id, first.listActivities(7, 17).single().id)
        assertTrue(first.listActivities(8).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            first.saveActivity(LessonActivityV7.Input(7, 18, "Turma errada", "Não pode vincular outra turma.", 20))
        }
        first.close()
        store = TeacherStore(context)
        val reopened = requireNotNull(store)
        assertEquals(LessonActivityV7.VERSION, reopened.readableDatabase.version)
        assertEquals(id, reopened.listActivities(7, 17).single().id)
        assertTrue(reopened.listActivities(8).isEmpty())
        assertEquals(31L, reopened.read().files.single().id)
        assertEquals(2, reopened.read().lessons.size)
        reopened.setLessonArchived(7, 17, true)
        assertThrows(IllegalArgumentException::class.java) {
            reopened.saveActivity(LessonActivityV7.Input(7, 17, "Plano arquivado", "Não permitir inclusão em plano arquivado.", 15))
        }
        assertEquals(1, reopened.listActivities(7).size)
    }
}
