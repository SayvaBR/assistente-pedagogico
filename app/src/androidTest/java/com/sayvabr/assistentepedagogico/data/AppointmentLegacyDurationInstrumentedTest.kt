package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic legacy v4 fixture; unknown historical durations are never invented. */
@RunWith(AndroidJUnit4::class)
class AppointmentLegacyDurationInstrumentedTest {
    private lateinit var db: SQLiteDatabase

    @Before fun prepare() {
        db = SQLiteDatabase.create(null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE classrooms (id INTEGER PRIMARY KEY, archived INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT INTO classrooms(id,archived) VALUES (1,0)")
        db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)")
        db.execSQL("INSERT INTO appointments(title,day,time) VALUES ('Legado sintético','2026-09-21','08:30')")
        AppointmentV5.migrate(db)
        db.execSQL("""CREATE TABLE lessons (id INTEGER PRIMARY KEY, classroom_id INTEGER NOT NULL REFERENCES classrooms(id),
            day TEXT NOT NULL, time TEXT NOT NULL, duration_minutes INTEGER NOT NULL DEFAULT 50,
            archived INTEGER NOT NULL DEFAULT 0, pedagogical_status TEXT NOT NULL DEFAULT 'draft')""")
    }

    @After fun cleanup() { db.close() }

    @Test fun migratedPointDoesNotBlockRealIntervalButRealOverlapIsRejected() {
        val newAppointment = AppointmentV5.Input("Novo compromisso", "2026-09-21", "08:00", "09:00", "Outro", 1)
        val id = AppointmentV5.create(db, newAppointment)
        assertEquals(2, db.rawQuery("SELECT COUNT(*) FROM appointments", null).use { c -> c.moveToFirst(); c.getInt(0) })
        db.rawQuery("SELECT time,end_time FROM appointments WHERE title='Legado sintético'", null).use { c ->
            c.moveToFirst()
            assertEquals("08:30", c.getString(0))
            assertEquals("08:30", c.getString(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppointmentV5.create(db, newAppointment.copy(startTime = "08:45", endTime = "09:15"))
        }
        assertEquals(2, db.rawQuery("SELECT COUNT(*) FROM appointments", null).use { c -> c.moveToFirst(); c.getInt(0) })
        assertEquals(id, db.rawQuery("SELECT id FROM appointments WHERE title='Novo compromisso'", null).use { c -> c.moveToFirst(); c.getLong(0) })
    }
}
