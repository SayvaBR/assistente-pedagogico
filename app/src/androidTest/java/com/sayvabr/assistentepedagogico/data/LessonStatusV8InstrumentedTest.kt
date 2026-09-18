package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator and synthetic data only; never delete a physical device's database. */
@RunWith(AndroidJUnit4::class)
class LessonStatusV8InstrumentedTest {
    private lateinit var context: Context
    private var store: TeacherStore? = null
    private var emulator = false

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        emulator = Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true)
        assumeTrue("Status migration tests require a disposable emulator", emulator)
        context.deleteDatabase("pedagogico.db")
        store = TeacherStore(context)
    }

    @After fun teardown() {
        store?.close()
        if (emulator) context.deleteDatabase("pedagogico.db")
    }

    private fun plan(title: String = "Aula fictícia") = LessonPlanV6.Input(
        title = title, subject = "Ciências", day = "2026-09-23", time = "08:00",
        durationMinutes = 60, objective = "Compreender um fenômeno fictício.",
        specificObjectives = "Registrar observações.", content = "Experimento sintético",
        bnccCodes = "", justification = "Continuidade do estudo", method = "Investigação coletiva",
        opening = "Apresentação", openingMinutes = 10,
        development = "Experimento", developmentMinutes = 40,
        closing = "Síntese", closingMinutes = 10,
        assessment = "Registro escrito", adaptations = "Material ampliado",
    )

    @Test fun transitionsPersistAndPreserveLinkedActivitiesAndClassIsolation() {
        val s = requireNotNull(store)
        val classroom = s.createClass("Turma fictícia A", "Ensino Fundamental", "Matutino")
        val other = s.createClass("Turma fictícia B", "Ensino Fundamental", "Vespertino")
        val lessonId = s.saveLesson(plan(), classroom)
        val activityId = s.saveActivity(LessonActivityV7.Input(classroom, lessonId,
            "Experimento sintético", "Registrar dados totalmente fictícios.", 20))
        assertEquals(LessonStatus.DRAFT, s.read().lessons.single().status)
        assertThrows(IllegalArgumentException::class.java) {
            s.transitionLessonStatus(other, lessonId, LessonStatus.READY)
        }
        assertEquals(LessonStatus.DRAFT, s.read().lessons.single().status)
        assertEquals(LessonStatus.READY, s.transitionLessonStatus(classroom, lessonId, LessonStatus.READY))
        assertEquals(LessonStatus.COMPLETED, s.transitionLessonStatus(classroom, lessonId, LessonStatus.COMPLETED))
        assertThrows(IllegalArgumentException::class.java) {
            s.transitionLessonStatus(classroom, lessonId, LessonStatus.DRAFT)
        }
        assertEquals(LessonStatus.ARCHIVED, s.transitionLessonStatus(classroom, lessonId, LessonStatus.ARCHIVED))
        assertTrue(s.read().lessons.single().archived)
        assertEquals(LessonStatus.COMPLETED, s.restoreLessonStatus(classroom, lessonId))
        assertEquals(activityId, s.listActivities(classroom, lessonId).single().id)
        assertTrue(s.listActivities(other).isEmpty())
        val copyId = s.duplicateLesson(classroom, lessonId)
        assertNotEquals(lessonId, copyId)
        assertEquals(LessonStatus.DRAFT, s.read().lessons.single { it.id == copyId }.status)
        assertEquals(LessonStatus.COMPLETED, s.read().lessons.single { it.id == lessonId }.status)
        s.close()
        store = TeacherStore(context)
        assertEquals(LessonStatus.COMPLETED, requireNotNull(store).read().lessons.single { it.id == lessonId }.status)
        assertEquals(activityId, requireNotNull(store).listActivities(classroom, lessonId).single().id)
    }

    @Test fun invalidOrSqlFailedTransitionDoesNotPartiallyChangeThePlan() {
        val s = requireNotNull(store)
        val classroom = s.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        val lessonId = s.saveLesson(plan(), classroom)
        val before = s.read()
        assertThrows(IllegalArgumentException::class.java) {
            s.transitionLessonStatus(classroom, lessonId, LessonStatus.COMPLETED)
        }
        assertEquals(before, s.read())
        s.writableDatabase.execSQL("""CREATE TEMP TRIGGER block_status BEFORE UPDATE OF pedagogical_status ON lessons
            WHEN NEW.id=$lessonId BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END""".trimIndent())
        assertThrows(android.database.SQLException::class.java) {
            s.transitionLessonStatus(classroom, lessonId, LessonStatus.READY)
        }
        assertEquals(before, s.read())
    }

    @Test fun backupRestoresDraftReadyCompletedAndArchivedPreviousState() {
        val s = requireNotNull(store)
        val classroom = s.createClass("Turma fictícia", "Ensino Fundamental", "Matutino")
        val draft = s.saveLesson(plan("Rascunho sintético"), classroom)
        val ready = s.saveLesson(plan("Pronto sintético"), classroom)
        val completed = s.saveLesson(plan("Concluído sintético"), classroom)
        val archived = s.saveLesson(plan("Arquivado sintético"), classroom)
        s.transitionLessonStatus(classroom, ready, LessonStatus.READY)
        s.transitionLessonStatus(classroom, completed, LessonStatus.READY)
        s.transitionLessonStatus(classroom, completed, LessonStatus.COMPLETED)
        s.transitionLessonStatus(classroom, archived, LessonStatus.READY)
        s.transitionLessonStatus(classroom, archived, LessonStatus.ARCHIVED)
        val payload = s.exportBackupPayload()
        val statusById = s.read().lessons.associate { it.id to it.status }
        assertEquals(setOf(LessonStatus.DRAFT, LessonStatus.READY, LessonStatus.COMPLETED, LessonStatus.ARCHIVED), statusById.values.toSet())
        val json = JSONObject(payload)
        val archivedRow = json.getJSONArray("lessons")
        val savedArchived = (0 until archivedRow.length()).map { archivedRow.getJSONObject(it) }.single { it.getLong("id") == archived }
        assertEquals("ready", savedArchived.getString("statusBeforeArchive"))
        assertThrows(IllegalArgumentException::class.java) {
            val invalid = JSONObject(payload)
            (0 until invalid.getJSONArray("lessons").length()).map { invalid.getJSONArray("lessons").getJSONObject(it) }
                .single { it.getLong("id") == draft }.put("pedagogicalStatus", "invented")
            s.restoreBackupAfterConfirmation(invalid.toString(), true)
        }
        assertEquals(statusById, s.read().lessons.associate { it.id to it.status })
        s.transitionLessonStatus(classroom, ready, LessonStatus.DRAFT)
        s.restoreBackupAfterConfirmation(payload, true)
        assertEquals(statusById, s.read().lessons.associate { it.id to it.status })
        assertEquals(LessonStatus.READY, s.restoreLessonStatus(classroom, archived))
        assertEquals(LessonStatus.DRAFT, s.read().lessons.single { it.id == draft }.status)
        s.close()
        store = TeacherStore(context)
        assertEquals(LessonStatus.COMPLETED, requireNotNull(store).read().lessons.single { it.id == completed }.status)
    }

    @Test fun v7MigrationPreservesIdsAndLegacyArchivedFlag() {
        store?.close()
        store = null
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
            LessonActivityV7.migrate(db)
            db.execSQL("INSERT INTO classrooms(id,name,stage,shift) VALUES(11,'Turma sintética','Ensino Fundamental','Matutino')")
            db.execSQL("INSERT INTO lessons(id,classroom_id,title,subject,day,time,objective,content,method,archived) VALUES(21,11,'Legado ativo','Ciências','2026-09-23','08:00','Objetivo sintético','Conteúdo','Método',0),(22,11,'Legado arquivado','Ciências','2026-09-23','09:00','Objetivo sintético','Conteúdo','Método',1)")
            db.execSQL("INSERT INTO lesson_activities(id,classroom_id,lesson_id,title,instructions,duration_minutes) VALUES(31,11,22,'Atividade legada','Instruções inteiramente fictícias.',20)")
            db.version = LessonActivityV7.VERSION
        } finally { db.close() }
        store = TeacherStore(context)
        val s = requireNotNull(store)
        assertEquals(LessonStatusV8.VERSION, s.readableDatabase.version)
        assertEquals(LessonStatus.DRAFT, s.read().lessons.single { it.id == 21L }.status)
        assertEquals(LessonStatus.ARCHIVED, s.read().lessons.single { it.id == 22L }.status)
        assertEquals(31L, s.listActivities(11, 22).single().id)
        assertEquals(LessonStatus.DRAFT, s.restoreLessonStatus(11, 22))
        s.close()
        store = TeacherStore(context)
        assertEquals(2, requireNotNull(store).read().lessons.size)
        assertEquals(31L, requireNotNull(store).listActivities(11, 22).single().id)
    }
}
