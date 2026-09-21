package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** These tests delete the app's database. Never execute them on a physical teacher device. */
@RunWith(AndroidJUnit4::class)
class TeacherStoreInstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var disposableEmulator = false

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        disposableEmulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.startsWith("sdk") ||
            Build.MODEL.contains("Emulator", ignoreCase = true)
        assumeTrue("Refusing destructive tests outside a disposable Android emulator", disposableEmulator)
        context.deleteDatabase("pedagogico.db")
    }

    @After fun tearDown() {
        store?.close()
        store = null
        if (disposableEmulator) context.deleteDatabase("pedagogico.db")
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
            // An invalid operation must leave persisted records untouched.
        }
    }

    @Test fun teacherRecordsSurviveDatabaseReopenAndEdits() {
        val first = db()
        first.saveProfile("Ana Maria")
        val classroomId = first.createClass("5º A", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Bruna")
        val studentId = first.read().students.single().id
        first.saveLesson(classroomId, "Frações", "Matemática", "2026-09-16", "08:00", "Comparar frações", "Frações", "Roda de conversa")
        first.saveAttendance(classroomId, "2026-09-16", mapOf(studentId to "P"))
        first.addObservation(classroomId, studentId, "Participação", "Participou da atividade.", false)
        first.addAppointment("Reunião pedagógica", "2026-09-16", "15:00")
        first.addFile("plano.pdf", "content://example/document/123")
        val saved = reopen().read()
        assertEquals("Ana Maria", saved.profile?.name)
        assertEquals("5º A", saved.classrooms.single().name)
        assertEquals("Bruna", saved.students.single().name)
        assertEquals("Frações", saved.lessons.single().title)
        assertEquals("P", saved.attendance.single().status)
        assertEquals(studentId, saved.observations.single().studentId)
        assertEquals("Reunião pedagógica", saved.appointments.single().title)
        assertEquals("plano.pdf", saved.files.single().name)
        db().saveProfile("Ana Beatriz")
        db().updateClass(classroomId, "5º B", "Ensino Fundamental", "Vespertino")
        db().updateStudent(classroomId, studentId, "Bruna Silva")
        val edited = reopen().read()
        assertEquals("Ana Beatriz", edited.profile?.name)
        assertEquals("5º B", edited.classrooms.single().name)
        assertEquals("Vespertino", edited.classrooms.single().shift)
        assertEquals("Bruna Silva", edited.students.single().name)
        assertEquals(1, edited.lessons.size)
        assertEquals(1, edited.attendance.size)
    }

    @Test fun archiveRestorePreserveHistoryAndEnforceTwoActiveClasses() {
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
        val saved = reopen().read()
        assertFalse(saved.classrooms.first { it.id == a }.archived)
        assertTrue(saved.classrooms.first { it.id == b }.archived)
        assertFalse(saved.classrooms.first { it.id == c }.archived)
        assertEquals(2, saved.classrooms.count { !it.archived })
        assertEquals(3, saved.classrooms.size)
        assertEquals("Catarina", saved.students.single().name)
        assertEquals("Leitura", saved.lessons.single().title)
    }

    @Test fun classOptionsAreValidatedOnCreateAndEditWithoutChangingSavedRecords() {
        val first = db()
        val id = first.createClass("Turma válida", "Ensino Fundamental", "Matutino")
        val unspecifiedShiftId = first.createClass("Turma sem turno", "Educação Infantil", ClassroomRules.unspecifiedShift)
        val before = first.read().classrooms

        rejects { first.createClass("Etapa inválida", "Curso não suportado", "Matutino") }
        rejects { first.createClass("Turno inválido", "Ensino Fundamental", "Integral") }
        rejects { first.updateClass(id, "Não salvar", "Curso não suportado", "Vespertino") }
        rejects { first.updateClass(id, "Não salvar", "Ensino Fundamental", "Integral") }
        assertEquals(before, reopen().read().classrooms)
        assertEquals(ClassroomRules.unspecifiedShift, before.single { it.id == unspecifiedShiftId }.shift)

        db().updateClass(id, "Turma atualizada", "Ensino Médio", "Noturno")
        val updated = reopen().read().classrooms.single { it.id == id }
        assertEquals("Turma atualizada", updated.name)
        assertEquals("Ensino Médio", updated.stage)
        assertEquals("Noturno", updated.shift)
    }

    @Test fun bulkStudentInsertPreservesHomonymsAndRejectsInvalidOrArchivedRosterAtomically() {
        val first = db()
        val classroomId = first.createClass("Turma da lista", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Aluna existente")

        rejects { first.addStudents(classroomId, listOf("Nova aluna", "X", "Outro aluno")) }
        assertEquals(listOf("Aluna existente"), reopen().read().students.map { it.name })

        val inserted = db().addStudents(classroomId, listOf(" Ana Silva ", "Bruno Souza", "Ana Silva"))
        assertEquals(3, inserted)
        val saved = reopen().read()
        assertEquals(listOf("Aluna existente", "Ana Silva", "Ana Silva", "Bruno Souza"), saved.students.map { it.name })

        db().setClassArchived(classroomId, true)
        rejects { db().addStudents(classroomId, listOf("Não adicionar")) }
        assertEquals(saved.students, reopen().read().students)
    }

    @Test fun studentDeletionKeepsHistoricalCallSnapshotAndUnlinksObservation() {
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
        val historical = saved.attendanceSessions.single()
        assertTrue(historical.rosterComplete)
        assertEquals(setOf("Daniel", "Elisa"), historical.members.map { it.studentName }.toSet())
        assertEquals("F", historical.members.single { it.studentName == "Daniel" }.status)
        assertEquals(1, saved.observations.size)
        assertNull(saved.observations.single().studentId)
    }

    @Test fun observationEditAndDeleteAreScopedAndPersist() {
        val first = db()
        val a = first.createClass("Turma Observação A", "Ensino Fundamental", "Matutino")
        val b = first.createClass("Turma Observação B", "Ensino Fundamental", "Vespertino")
        first.addStudent(a, "Iara")
        first.addStudent(b, "João")
        val iara = first.read().students.first { it.classroomId == a }.id
        val joao = first.read().students.first { it.classroomId == b }.id
        first.addObservation(a, iara, "Participação", "Registro original da aluna.", false)
        val note = first.read().observations.single()
        rejects { first.updateObservation(b, note.id, joao, "Aprendizagem", "Tentativa indevida.", true) }
        rejects { first.updateObservation(a, note.id, joao, "Aprendizagem", "Aluno de outra turma.", true) }
        first.updateObservation(a, note.id, null, "Aprendizagem", "Registro pedagógico revisado.", true)
        var saved = reopen().read().observations.single()
        assertEquals(note.id, saved.id)
        assertNull(saved.studentId)
        assertEquals("Aprendizagem", saved.kind)
        assertEquals("Registro pedagógico revisado.", saved.body)
        assertTrue(saved.shareApproved)
        rejects { db().deleteObservation(b, note.id) }
        assertEquals(1, reopen().read().observations.size)
        db().deleteObservation(a, note.id)
        assertTrue(reopen().read().observations.isEmpty())
        rejects { db().deleteObservation(a, note.id) }
    }

    @Test fun invalidAttendanceBatchRollsBackAllMarks() {
        val first = db()
        val classroomId = first.createClass("5º C", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Fernanda")
        first.addStudent(classroomId, "Gabriel")
        val ids = first.read().students.map { it.id }
        first.saveAttendance(classroomId, "2026-09-16", ids.associateWith { "P" })
        val before = first.read().attendanceSessions
        rejects { first.saveAttendance(classroomId, "2026-09-16", mapOf(ids[0] to "F", ids[1] to "INVALID")) }
        assertTrue(reopen().read().attendance.all { it.status == "P" })
        assertEquals(before, reopen().read().attendanceSessions)
    }

    @Test fun attendanceSessionSnapshotsRosterAndRejectsStudentsAddedLater() {
        val first = db()
        val classroomId = first.createClass("5º D", "Ensino Fundamental", "Matutino")
        first.addStudent(classroomId, "Helena")
        first.addStudent(classroomId, "Igor")
        val beforeCall = first.read().students.associateBy { it.name }
        val helena = beforeCall.getValue("Helena").id
        val igor = beforeCall.getValue("Igor").id

        first.saveAttendance(classroomId, "2026-09-17", mapOf(helena to "P"))
        first.addStudent(classroomId, "Júlia")
        val thirdStudent = first.read().students.single { it.name == "Júlia" }.id
        var session = reopen().read().attendanceSessions.single()
        assertTrue(session.rosterComplete)
        assertEquals(setOf(helena, igor), session.members.map { it.studentId }.toSet())
        assertEquals("?", session.members.single { it.studentId == igor }.status)
        rejects { db().saveAttendance(classroomId, "2026-09-17", mapOf(thirdStudent to "P")) }

        db().saveAttendance(classroomId, "2026-09-17", mapOf(helena to "F"))
        db().deleteStudent(classroomId, helena)
        session = reopen().read().attendanceSessions.single()
        assertEquals(setOf(helena, igor), session.members.map { it.studentId }.toSet())
        assertEquals("Helena", session.members.single { it.studentId == helena }.studentName)
        assertEquals("F", session.members.single { it.studentId == helena }.status)
    }

    @Test fun migrationFromV9KeepsOnlyKnownMarksAsAnIncompleteHistoricalRoster() {
        val file = context.getDatabasePath("pedagogico.db")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            TeacherStore(context).onCreate(old)
            old.execSQL("DROP TABLE attendance_session_members")
            old.execSQL("DROP TABLE attendance_sessions")
            old.execSQL("INSERT INTO classrooms(id,name,stage,shift,archived) VALUES (71,'Turma v9','Ensino Fundamental','Matutino',0)")
            old.execSQL("INSERT INTO students(id,classroom_id,name) VALUES (72,71,'Marca preservada'),(73,71,'Sem marca antiga')")
            old.execSQL("INSERT INTO attendance(classroom_id,student_id,day,status) VALUES (71,72,'2026-09-16','P')")
            old.version = 9
        }

        val migrated = db().read()
        val session = migrated.attendanceSessions.single()
        assertFalse(session.rosterComplete)
        assertEquals("2026-09-16", session.date)
        assertEquals(listOf("Marca preservada"), session.members.map { it.studentName })
        db().addStudent(71, "Adicionada depois")
        assertEquals(session, reopen().read().attendanceSessions.single())
    }

    @Test fun fileCatalogRenameAndRemovalPersistWithoutTouchingOriginalUri() {
        val first = db()
        first.addFile("original.pdf", "content://example/document/777")
        val file = first.read().files.single()
        first.renameFile(file.id, "Plano de aula.pdf")
        val renamed = reopen().read().files.single()
        assertEquals("Plano de aula.pdf", renamed.name)
        assertEquals("content://example/document/777", renamed.uri)
        rejects { db().renameFile(file.id, "   ") }
        assertEquals("Plano de aula.pdf", reopen().read().files.single().name)
        db().removeFile(file.id)
        assertTrue(reopen().read().files.isEmpty())
        rejects { db().removeFile(file.id) }
    }

    @Test fun lessonEditAndArchiveRestorePreserveIdentityAndClassIsolation() {
        val first = db()
        val a = first.createClass("Turma A", "Ensino Fundamental", "Matutino")
        val b = first.createClass("Turma B", "Ensino Médio", "Vespertino")
        first.saveLesson(a, "Plano original", "História", "2026-09-16", "08:00", "Aprender", "Tema", "Discussão")
        val original = first.read().lessons.single()
        rejects { first.updateLesson(b, original.id, "Indevido", "História", "2026-09-17", "10:00", "Aprender", "Tema", "Discussão") }
        rejects { first.setLessonArchived(b, original.id, true) }
        assertEquals("Plano original", first.read().lessons.single().title)
        first.updateLesson(a, original.id, "Plano revisado", "Geografia", "2026-09-18", "09:30", "Pesquisar", "Mapas", "Pesquisa de campo")
        var saved = reopen().read().lessons.single()
        assertEquals(original.id, saved.id)
        assertEquals("Plano revisado", saved.title)
        assertEquals("Geografia", saved.subject)
        assertEquals("2026-09-18", saved.date)
        assertEquals("09:30", saved.time)
        assertFalse(saved.archived)
        db().setLessonArchived(a, original.id, true)
        saved = reopen().read().lessons.single()
        assertTrue(saved.archived)
        rejects { db().updateLesson(a, original.id, "Inválido", "Geografia", "2026-09-18", "09:30", "Pesquisar", "Mapas", "Pesquisa") }
        db().setLessonArchived(a, original.id, false)
        saved = reopen().read().lessons.single()
        assertFalse(saved.archived)
        assertEquals("Plano revisado", saved.title)
        assertEquals(original.id, saved.id)
    }

    @Test fun migrationV2ToV3KeepsLessonAndAddsArchiveFlag() {
        val file = context.getDatabasePath("pedagogico.db")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
  TeacherStore(context).onCreate(old)
  old.execSQL("DROP TABLE lessons")
  old.execSQL("CREATE TABLE lessons (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, title TEXT NOT NULL, subject TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL, objective TEXT NOT NULL, content TEXT NOT NULL, method TEXT NOT NULL)")
  old.execSQL("INSERT INTO classrooms (id,name,stage,shift,archived) VALUES (1,'Turma v2','Educação Infantil','Matutino',0)")
  old.execSQL("INSERT INTO lessons (classroom_id,title,subject,day,time,objective,content,method) VALUES (1,'Plano v2','Linguagem','2026-09-16','08:00','Objetivo','Conteúdo','Método')")
  old.version = 2
        }
        val migrated = db().read()
        assertEquals("Plano v2", migrated.lessons.single().title)
        assertFalse(migrated.lessons.single().archived)
        db().setLessonArchived(1L, migrated.lessons.single().id, true)
        assertTrue(reopen().read().lessons.single().archived)
    }

    @Test fun migrationV1ToV2PreservesEveryExistingTable() {
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
        assertFalse(migrated.lessons.single().archived)
        assertEquals("P", migrated.attendance.single().status)
        assertEquals("Observação antiga", migrated.observations.single().body)
        assertEquals("Agenda antiga", migrated.appointments.single().title)
        assertEquals("antigo.pdf", migrated.files.single().name)
        db().setClassArchived(1L, true)
        assertTrue(reopen().read().classrooms.single().archived)
    }
}
