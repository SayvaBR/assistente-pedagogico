package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentV5InstrumentedTest {
    private fun legacyDb(): SQLiteDatabase = SQLiteDatabase.create(null).apply {
        execSQL("CREATE TABLE classrooms (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
        execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)")
        execSQL("INSERT INTO classrooms(id,name) VALUES (7,'Turma sintética')")
        execSQL("INSERT INTO appointments(id,title,day,time) VALUES (41,'Legado','2026-09-18','09:00')")
    }

    @Test fun migration_isAdditiveIdempotentAndPreservesLegacyRow() {
        legacyDb().use { db ->
            AppointmentV5.migrate(db)
            AppointmentV5.migrate(db)
            db.rawQuery("SELECT id,title,day,time,end_time,type,classroom_id FROM appointments WHERE id=41", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(41L, c.getLong(0))
                assertEquals("Legado", c.getString(1))
                assertEquals("2026-09-18", c.getString(2))
                assertEquals("09:00", c.getString(3))
                assertEquals("09:00", c.getString(4))
                assertEquals("Outro", c.getString(5))
                assertTrue(c.isNull(6))
            }
        }
    }

    @Test fun conflict_detectsOverlapButAllowsTouchingBoundary() {
        legacyDb().use { db ->
            AppointmentV5.migrate(db)
            db.execSQL("UPDATE appointments SET end_time='10:00', type='Reunião', classroom_id=7 WHERE id=41")
            assertTrue(AppointmentV5.hasConflict(db, AppointmentV5.Input("Conflito", "2026-09-18", "09:30", "10:30", "Evento", 7)))
            assertFalse(AppointmentV5.hasConflict(db, AppointmentV5.Input("Depois", "2026-09-18", "10:00", "11:00", "Evento", 7)))
            assertFalse(AppointmentV5.hasConflict(db, AppointmentV5.Input("Outra turma", "2026-09-18", "09:30", "10:30", "Evento", 8)))
        }
    }

    @Test fun validationRejectsInvalidRangeAndType() {
        val badRange = runCatching { AppointmentV5.validated(AppointmentV5.Input("Teste", "2026-09-18", "10:00", "09:00", "Evento", null)) }
        assertTrue(badRange.isFailure)
        val badType = runCatching { AppointmentV5.validated(AppointmentV5.Input("Teste", "2026-09-18", "09:00", "10:00", "Inventado", null)) }
        assertTrue(badType.isFailure)
    }
}
