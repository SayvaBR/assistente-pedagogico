package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Run ONLY on a disposable emulator: each test deletes the app's test database. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store?.close()
        context.deleteDatabase("pedagogico.db")
    }

    @After fun tearDown() {
        store?.close()
        store = null
        context.deleteDatabase("pedagogico.db")
    }

    private fun db(): TeacherStore = store ?: TeacherStore(context).also { store = it }

    private fun reopen(): TeacherStore {
        store?.close()
        store = TeacherStore(context)
        return store!!
    }

    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Expected an IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // A rejected operation must leave persisted records unchanged.
        }
    }

    @Test fun profileClassStudentLessonAttendanceObservationAgendaAndFileSurviveReopenAndEdit() {
        val first = db()
        first.saveProfile("Ana Maria")
        val classId = first.createClass("5º A", "Ensino Fundamental", "Matutino")
        first.addStudent(classId, "Bruna")
        val studentId = first.read().students.single().id
        first.saveLesson(classId, "Frações", "Matemática", "2026-09-16", "08:00", "Comparar frações", "Frações", "Roda de conversa")
        first.saveAttendance(classId, "2026-09-16", mapOf(studentId to "P"))
        first.addObservation(classId, studentId, "Participação", "Participou da atividade.", false)
        first.addAppointment("Reunião pedagógica", "2026-09-16", "15:00")
        first.addFile("plano.pdf", "content://example/document/123")

        val second = reopen()
        val saved = second.read()
        assertEquals("Ana Maria", saved.profile?.name)
        assertEquals("5º A", saved.classrooms.single().name)
        assertEquals("Bruna", saved.students.single().name)
        assertEquals("Frações", saved.lessons.single().title)
        assertEquals("P", saved.attendance.single().status)
        assertEquals(studentId, saved.observations.single().studentId)
        assertEquals("Reunião pedagógica", saved.appointments.single().title)
        assertEquals("plano.pdf", saved.files.single().name)

        second.saveProfile("Ana Beatriz")
        second.updateClass(classId, "5º B", "Ensino Fundamental", "Vespertino")
        second.updateStudent(classId, studentId, "Bruna Silva")
        val edited = reopen().read()
        assertEquals("Ana Beatriz", edited.profile?.name)
        assertEquals("5º B", edited.classrooms.single().name)
        assertEquals("Vespertino", edited.classrooms.single().shift)
        assertEquals("Bruna Silva", edited.students.single().name)
        assertEquals(1, edited.lessons.size)
        assertEquals(1, edited.attendance.size)
    }

    @Test fun archiveAndRestoreKeepRecordsAndOnlyTwoActiveClassesAreAllowed() {
        val first = db()
        val a = first.createClass("Turma A", "Educação Infantil", "Matutino")
        first.addStudent(a, "Catarina")
        first.saveLesson(a, "Leitura", "Linguagem", "2026-09-16", "09:00", "Ler", "Texto", "Leitura coletiva")
        val b = first.createClass("Turma B", "Ensino Médio", "Noturno")
        rejects { first.createClass("Turma bloqueada", "Ensino Médio", "Noturno") }
        first.setClassArchived(a, true)
        val c = first.createClass("Turma C", "Ensino Fundamental", "Vespertino")
        rejects { first.setClassArchived(a, false) }
        assertTrue(first.read().classrooms.first { it.id == a }.archived)
        first.setClassArchived(b, true)
        first.setClassArchived(a, false)

        val persisted = reopen().read()
        assertFalse(persisted.classrooms.first { it.id == a }.archived)
        assertTrue(persisted.classrooms.first { it.id == b }.archived)
        assertFalse(persisted.classrooms.first { it.id == c }.archived)
        assertEquals(2, persisted.classrooms.count { !it.archived })
        assertEquals("Catarina", persisted.students.single().name)
        assertEquals("Leitura", persisted.lessons.single().title)
        assertEquals(3, persisted.classrooms.size)
    }

    @Test fun deletingStudentCascadesAttendanceButPreservesObservationAndOtherStudents() {
        val first = db()
        val classroomId = first.createClass("4º A", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Daniel")
        first.addStudent(classroomId, "Elisa")
        val students = first.read().students.associateBy { it.name }
        val daniel = students.getValue("Daniel").id
        val elisa = students.getValue("Elisa").id
        first.saveAttendance(classroomId, "2026-09-16", mapOf(daniel to "F", elisa to "P"))
        first.addObservation(classroomId, daniel, "Aprendizagem", "Registro de progresso.", false)
        first.deleteStudent(classroomId, daniel)
        val saved = reopen().read()
        assertEquals(listOf("Elisa"), saved.students.map { it.name })
        assertEquals(listOf(elisa), saved.attendance.map { it.studentId })
        assertEquals("P", saved.attendance.single().status)
        assertEquals(1, saved.observations.size)
        assertNull(saved.observations.single().studentId)
    }

    @Test fun invalidAttendanceRollsBackEntireBatch() {
        val first = db()
        val classroomId = first.createClass("5º C", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Fernanda")
        first.addStudent(classroomId, "Gabriel")
        val ids = first.read().students.map { it.id }
        first.saveAttendance(classroomId, "2026-09-16", ids.associateWith { "P" })
        rejects { first.saveAttendance(classroomId, "2026-09-16", mapOf(ids[0] to "F", ids[1] to "INVALID")) }
        assertTrue(reopen().read().attendance.all { it.status == "P" })
    }

    @Test fun databaseV1MigratesToV2WithoutLosingAnyTable() {
        // Recreate the exact v1 schema to test upgrades from an already-installed application.
        val file = context.getDatabasePath("pedagogico.db")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            listOf(
                "CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL)",
                "CREATE TABLE classrooms (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, stage TEXT NOT NULL, shift TEXT NOT NULL)",
                "CREATE TABLE students (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, name TEXT NOT NULL)",
                "CREATE TABLE lessons (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, title TEXT NOT NULL, subject TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL, objective TEXT NOT NULL, content TEXT NOT NULL, method TEXT NOT NULL)",
                "CREATE TABLE attendance (classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE, day TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('P','F')), PRIMARY KEY(student_id,day))",
                "CREATE TABLE observations (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER REFERENCES students(id) ON DELETE SET NULL, kind TEXT NOT NULL, body TEXT NOT NULL, day TEXT NOT NULL, share_approved INTEGER NOT NULL DEFAULT 0)",
                "CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)",
                "CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)",
            ).forEach(old::execSQL)
            old.execSQL("INSERT INTO profile (id,name) VALUES (1,'Docente anterior')")
            old.execSQL("INSERT INTO classrooms (id,name,stage,shift) VALUES (1,'Turma antiga','Educação Infantil','Matutino')")
            old.execSQL("INSERT INTO students (id,classroom_id,name) VALUES (1,1,'Helena')")
            old.execSQL("INSERT INTO lessons (classroom_id,title,subject,day,time,objective,content,method) VALUES (1,'Aula antiga','Linguagem','2026-09-16','09:00','Objetivo','Conteúdo','Método')")
            old.execSQL("INSERT INTO attendance (classroom_id,student_id,day,status) VALUES (1,1,'2026-09-16','P')")
            old.execSQL("INSERT INTO observations (classroom_id,student_id,kind,body,day,share_approved) VALUES (1,1,'Participação','Observação antiga','2026-09-16',0)")
            old.execSQL("INSERT INTO appointments (title,day,time) VALUES ('Agenda antiga','2026-09-16','10:00')")
            old.execSQL("INSERT INTO saved_files (name,uri) VALUES ('antigo.pdf','content://example/old')")
            old.version = 1
        }
        val migrated = db().read()
        assertEquals("Docente anterior", migrated.profile?.name)
        assertEquals("Turma antiga", migrated.classrooms.single().name)
        assertFalse(migrated.classrooms.single().archived)
        assertEquals("Helena", migrated.students.single().name)
        assertEquals("Aula antiga", migrated.lessons.single().title)
        assertEquals("P", migrated.attendance.single().status)
        assertEquals("Observação antiga", migrated.observations.single().body)
        assertEquals("Agenda antiga", migrated.appointments.single().title)
        assertEquals("antigo.pdf", migrated.files.single().name)
        db().setClassArchived(1L, true)
        assertTrue(reopen().read().classrooms.single().archived)
    }
}
