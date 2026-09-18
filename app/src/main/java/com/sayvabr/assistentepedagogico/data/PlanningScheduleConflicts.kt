package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import java.time.LocalTime

/** A draft is not a booked lesson. Only ready/completed lessons in active classes reserve time.
 * Intervals are half-open: a lesson ending at 09:00 can precede a 09:00 appointment.
 * Global appointments affect every class; class-scoped appointments affect only their class.
 */
object PlanningScheduleConflicts {
    fun lessonBlocksAppointment(db: SQLiteDatabase, input: AppointmentV5.Input): Boolean {
        val startMinute = LocalTime.parse(input.startTime).toSecondOfDay() / 60
        val args = mutableListOf(input.day, input.endTime, startMinute.toString())
        val classroomFilter = if (input.classroomId == null) "1=1" else {
            args += input.classroomId.toString()
            "l.classroom_id=?"
        }
        db.rawQuery(
            """SELECT 1 FROM lessons l JOIN classrooms c ON c.id=l.classroom_id
               WHERE l.day=? AND l.archived=0 AND c.archived=0
                 AND l.pedagogical_status IN ('ready','completed') AND l.time < ?
                 AND (CAST(substr(l.time,1,2) AS INTEGER)*60 +
                      CAST(substr(l.time,4,2) AS INTEGER) + l.duration_minutes) > CAST(? AS INTEGER)
                 AND $classroomFilter LIMIT 1""".trimIndent(),
            args.toTypedArray(),
        ).use { return it.moveToFirst() }
    }

    /** Called inside the existing status transaction, including unarchive to ready/completed. */
    fun appointmentBlocksLesson(db: SQLiteDatabase, classroomId: Long, lessonId: Long): Boolean {
        val (day, start, end) = db.rawQuery(
            "SELECT day,time,duration_minutes FROM lessons WHERE id=? AND classroom_id=?",
            arrayOf(lessonId.toString(), classroomId.toString()),
        ).use { c ->
            require(c.moveToFirst()) { "Plano não encontrado nesta turma." }
            val time = c.getString(1)
            Triple(c.getString(0), time, LessonPlanV6.endTime(time, c.getInt(2)))
        }
        db.rawQuery(
            """SELECT 1 FROM appointments WHERE day=? AND time < ? AND end_time > ?
                 AND end_time > time AND (classroom_id IS NULL OR classroom_id=?) LIMIT 1""".trimIndent(),
            arrayOf(day, end, start, classroomId.toString()),
        ).use { return it.moveToFirst() }
    }
}
