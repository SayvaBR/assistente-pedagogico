package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated in-memory SQLite fixtures: never open, overwrite or delete a teacher database. */
@RunWith(AndroidJUnit4::class)
class AppointmentConflictInstrumentedTest {
    private lateinit var db: SQLiteDatabase

    @Before fun prepare() {
        db = SQLiteDatabase.create(null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE classrooms (id INTEGER PRIMARY KEY, archived INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO classrooms(id,archived) VALUES (1,0),(2,0),(3,1)")
        db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)")
        AppointmentV5.migrate(db)
        // Production always has lessons; include its planning columns in this isolated fixture.
        db.execSQL("""CREATE TABLE lessons (id INTEGER PRIMARY KEY, classroom_id INTEGER NOT NULL REFERENCES classrooms(id),
            day TEXT NOT NULL, time TEXT NOT NULL, duration_minutes INTEGER NOT NULL DEFAULT 50,
            archived INTEGER NOT NULL DEFAULT 0, pedagogical_status TEXT NOT NULL DEFAULT 'draft')""")
    }

    @After fun cleanup() { db.close() }

    private fun input(start: String = "08:00", end: String = "09:00", classroom: Long? = 1L,
        type: String = "Reunião") = AppointmentV5.Input("Reunião com professores", "2026-09-21", start, end, type, classroom)

    private fun rejected(block: () -> Unit) {
        try { block(); fail("A operação inválida deveria ser rejeitada") }
        catch (_: IllegalArgumentException) { /* Expected and transaction is rolled back. */ }
    }

    private fun count(): Int = db.rawQuery("SELECT COUNT(*) FROM appointments", null).use { c ->
        c.moveToFirst(); c.getInt(0)
    }

    @Test fun sameClassOverlapRejectedAndTouchingOrOtherClassAllowed() {
        val id = AppointmentV5.create(db, input())
        rejected { AppointmentV5.create(db, input("08:30", "09:15")) }
        rejected { AppointmentV5.create(db, input("07:30", "08:30")) }
        assertEquals(1, count())
        AppointmentV5.create(db, input("09:00", "10:00"))
        AppointmentV5.create(db, input("08:30", "09:15", classroom = 2L))
        assertEquals(3, count())
        AppointmentV5.update(db, id, input("08:00", "08:30")) // Excludes its own ID.
        db.rawQuery("SELECT end_time FROM appointments WHERE id=?", arrayOf(id.toString())).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("08:30", c.getString(0))
        }
    }

    @Test fun globalEventsBlockClassesAndFailedEditRetainsOriginalValues() {
        AppointmentV5.create(db, input())
        rejected { AppointmentV5.create(db, input("08:20", "08:50", classroom = null)) }
        val globalId = AppointmentV5.create(db, input("13:00", "14:00", classroom = null))
        rejected { AppointmentV5.create(db, input("13:30", "14:30", classroom = 2L)) }
        rejected { AppointmentV5.update(db, globalId, input("08:10", "08:50", classroom = null)) }
        db.rawQuery("SELECT time,end_time,classroom_id FROM appointments WHERE id=?", arrayOf(globalId.toString())).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("13:00", c.getString(0))
            assertEquals("14:00", c.getString(1))
            assertTrue(c.isNull(2))
        }
        assertEquals(2, count())
    }

    @Test fun invalidRangesArchivedClassesAndBadCategoryCannotPersist() {
        rejected { AppointmentV5.create(db, input("10:00", "09:00")) }
        rejected { AppointmentV5.create(db, input("08:00", "08:00")) }
        rejected { AppointmentV5.create(db, input(classroom = 999L)) }
        rejected { AppointmentV5.create(db, input(classroom = 3L)) }
        rejected { AppointmentV5.create(db, input(type = "Sem categoria válida")) }
        assertEquals(0, count())
    }
}
