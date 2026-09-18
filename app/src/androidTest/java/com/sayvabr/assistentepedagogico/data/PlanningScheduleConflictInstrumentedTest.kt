package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-memory fixtures verify both directions of the lesson/appointment conflict rule. */
@RunWith(AndroidJUnit4::class)
class PlanningScheduleConflictInstrumentedTest {
    private lateinit var db: SQLiteDatabase

    @Before fun prepare() {
        db = SQLiteDatabase.create(null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE classrooms (id INTEGER PRIMARY KEY, archived INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO classrooms(id,archived) VALUES (1,0),(2,0),(3,1)")
        db.execSQL("""CREATE TABLE lessons (id INTEGER PRIMARY KEY AUTOINCREMENT,
            classroom_id INTEGER NOT NULL REFERENCES classrooms(id), title TEXT NOT NULL,
            subject TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL,
            objective TEXT NOT NULL, content TEXT NOT NULL, method TEXT NOT NULL,
            archived INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)")
        AppointmentV5.migrate(db)
        LessonPlanV6.migrate(db)
        LessonStatusV8.migrate(db)
    }

    @After fun cleanup() { db.close() }

    private fun lesson(classroom: Long = 1, time: String = "08:00"): Long {
        val values = ContentValues().apply {
            put("classroom_id", classroom)
            put("title", "Aula sintética")
            put("subject", "Ciências")
            put("day", "2026-09-21")
            put("time", time)
            put("duration_minutes", 60)
            put("objective", "Compreender fenômeno sintético")
            put("content", "Conteúdo fictício")
            put("method", "Investigação")
            put("opening", "Início")
            put("opening_minutes", 10)
            put("development", "Atividade")
            put("development_minutes", 40)
            put("closing", "Síntese")
            put("closing_minutes", 10)
        }
        return db.insertOrThrow("lessons", null, values)
    }

    private fun appointment(start: String, end: String, classroom: Long? = 1) =
        AppointmentV5.Input("Compromisso sintético", "2026-09-21", start, end, "Outro", classroom)

    private fun status(id: Long): String = db.rawQuery(
        "SELECT pedagogical_status FROM lessons WHERE id=?", arrayOf(id.toString()),
    ).use { c -> c.moveToFirst(); c.getString(0) }

    @Test fun readyLessonBlocksMatchingAndGlobalAppointmentsButNotOtherClassesOrTouchingEdges() {
        val id = lesson()
        LessonStatusV8.transition(db, 1, id, LessonStatus.READY)
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.create(db, appointment("08:30", "09:30"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.create(db, appointment("08:30", "09:30", null))
        }
        val other = AppointmentV5.create(db, appointment("08:30", "09:30", 2))
        AppointmentV5.create(db, appointment("09:00", "09:30"))
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.update(db, other, appointment("08:30", "09:30", 1))
        }
        db.rawQuery("SELECT classroom_id FROM appointments WHERE id=?", arrayOf(other.toString())).use { c ->
            c.moveToFirst()
            assertEquals(2L, c.getLong(0))
        }
        LessonStatusV8.transition(db, 1, id, LessonStatus.COMPLETED)
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.create(db, appointment("07:30", "08:30"))
        }
        LessonStatusV8.transition(db, 1, id, LessonStatus.ARCHIVED)
        AppointmentV5.create(db, appointment("08:00", "08:30"))
        assertThrows(IllegalArgumentException::class.java) { LessonStatusV8.restore(db, 1, id) }
        assertEquals("archived", status(id))
    }

    @Test fun draftIsEditableButCannotBecomeReadyUntilConflictingCommitmentMoves() {
        val id = lesson()
        val appointmentId = AppointmentV5.create(db, appointment("08:30", "09:30"))
        assertEquals("draft", status(id))
        assertThrows(IllegalArgumentException::class.java) {
            LessonStatusV8.transition(db, 1, id, LessonStatus.READY)
        }
        assertEquals("draft", status(id))
        AppointmentV5.update(db, appointmentId, appointment("09:00", "10:00"))
        assertEquals(LessonStatus.READY, LessonStatusV8.transition(db, 1, id, LessonStatus.READY))
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.update(db, appointmentId, appointment("08:30", "09:30"))
        }
        db.rawQuery("SELECT time FROM appointments WHERE id=?", arrayOf(appointmentId.toString())).use { c ->
            c.moveToFirst()
            assertEquals("09:00", c.getString(0))
        }
    }

    @Test fun differentClassDoesNotBlockReadinessGlobalCommitmentDoesAndLegacyPointIsNotInvented() {
        AppointmentV5.create(db, appointment("08:30", "09:30", 2))
        val id = lesson(1)
        LessonStatusV8.transition(db, 1, id, LessonStatus.READY)
        val other = lesson(2, "11:00")
        AppointmentV5.create(db, appointment("11:15", "11:45", null))
        assertThrows(IllegalArgumentException::class.java) {
            LessonStatusV8.transition(db, 2, other, LessonStatus.READY)
        }
        assertEquals("draft", status(other))
        val third = lesson(1, "13:00")
        db.execSQL("INSERT INTO appointments(title,day,time,end_time,type) VALUES ('legado','2026-09-21','13:20',NULL,'Outro')")
        assertEquals(LessonStatus.READY, LessonStatusV8.transition(db, 1, third, LessonStatus.READY))
    }
}
