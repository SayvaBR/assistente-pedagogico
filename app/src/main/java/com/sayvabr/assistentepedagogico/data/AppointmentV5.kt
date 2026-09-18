package com.sayvabr.assistentepedagogico.data

import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime

/** Additive v5 contract for commitments. Existing v4 rows remain valid and keep their IDs. */
object AppointmentV5 {
    const val VERSION = 5
    val TYPES = setOf("Reunião", "Prazo", "Evento", "Pessoal", "Outro")

    data class Input(
        val title: String,
        val day: String,
        val startTime: String,
        val endTime: String,
        val type: String,
        val classroomId: Long?,
    )

    fun migrate(db: SQLiteDatabase) {
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(appointments)", null).use { c ->
            val name = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) columns += c.getString(name)
        }
        if ("end_time" !in columns) db.execSQL("ALTER TABLE appointments ADD COLUMN end_time TEXT")
        if ("type" !in columns) db.execSQL("ALTER TABLE appointments ADD COLUMN type TEXT NOT NULL DEFAULT 'Outro'")
        if ("classroom_id" !in columns) db.execSQL("ALTER TABLE appointments ADD COLUMN classroom_id INTEGER REFERENCES classrooms(id) ON DELETE SET NULL")
        // Legacy rows had only a start time. A zero-duration legacy value is explicit and can be edited later.
        db.execSQL("UPDATE appointments SET end_time=time WHERE end_time IS NULL OR trim(end_time)='' ")
    }

    fun validated(input: Input): Input {
        val title = input.title.trim()
        require(title.isNotEmpty()) { "Informe o compromisso." }
        require(title.length <= 120) { "O título deve ter até 120 caracteres." }
        LocalDate.parse(input.day)
        val start = LocalTime.parse(input.startTime)
        val end = LocalTime.parse(input.endTime)
        require(end > start) { "A hora final deve ser depois da hora inicial." }
        require(input.type in TYPES) { "Tipo de compromisso inválido." }
        require(input.classroomId == null || input.classroomId > 0) { "Turma inválida." }
        return input.copy(title = title)
    }

    /** Returns true when the candidate overlaps another commitment on the same day.
     * Classroom-less commitments conflict globally; classroom-bound commitments conflict only
     * with the same classroom or a global commitment. Touching boundaries are allowed.
     */
    fun hasConflict(db: SQLiteDatabase, input: Input, excludingId: Long? = null): Boolean {
        val value = validated(input)
        val args = mutableListOf(value.day, value.startTime, value.endTime)
        val classroomClause = if (value.classroomId == null) {
            "1=1"
        } else {
            args += value.classroomId.toString()
            "(classroom_id IS NULL OR classroom_id=?)"
        }
        val excludeClause = if (excludingId == null) "" else {
            args += excludingId.toString()
            " AND id<>?"
        }
        val sql = "SELECT 1 FROM appointments WHERE day=? AND time < ? AND end_time > ? AND $classroomClause$excludeClause LIMIT 1"
        db.rawQuery(sql, args.toTypedArray()).use { return it.moveToFirst() }
    }
}
