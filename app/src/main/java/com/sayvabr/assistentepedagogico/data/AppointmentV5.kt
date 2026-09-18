package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Additive appointment storage. v4 records retain their IDs, title, date and start time. */
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
        // A legacy appointment has no known duration; keep its recorded time, never fabricate one.
        db.execSQL("UPDATE appointments SET end_time=time WHERE end_time IS NULL OR trim(end_time)=''")
    }

    fun validated(input: Input): Input {
        val title = input.title.trim()
        require(title.isNotEmpty()) { "Informe o compromisso." }
        require(title.length <= 120) { "O título deve ter até 120 caracteres." }
        LocalDate.parse(input.day)
        val start = LocalTime.parse(input.startTime)
        val end = LocalTime.parse(input.endTime)
        require(start.format(DateTimeFormatter.ofPattern("HH:mm")) == input.startTime &&
            end.format(DateTimeFormatter.ofPattern("HH:mm")) == input.endTime) { "Informe horários no formato HH:MM." }
        require(end > start) { "A hora final deve ser depois da hora inicial." }
        require(input.type in TYPES) { "Tipo de compromisso inválido." }
        require(input.classroomId == null || input.classroomId > 0) { "Turma inválida." }
        return input.copy(title = title)
    }

    private fun requireActiveClass(db: SQLiteDatabase, id: Long?) {
        if (id == null) return
        db.rawQuery("SELECT archived FROM classrooms WHERE id=?", arrayOf(id.toString())).use { c ->
            require(c.moveToFirst() && c.getInt(0) == 0) { "Turma não encontrada ou arquivada." }
        }
    }

    /** Half-open intervals: [start, end). Different classes may schedule in parallel, but
     * a commitment without a classroom blocks all classes on the same day.
     */
    fun hasConflict(db: SQLiteDatabase, input: Input, excludingId: Long? = null): Boolean {
        val value = validated(input)
        val args = mutableListOf(value.day, value.endTime, value.startTime)
        val classroomClause = if (value.classroomId == null) "1=1" else {
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

    private fun values(input: Input) = ContentValues().apply {
        put("title", input.title)
        put("day", input.day)
        put("time", input.startTime)
        put("end_time", input.endTime)
        put("type", input.type)
        if (input.classroomId == null) putNull("classroom_id") else put("classroom_id", input.classroomId)
    }

    fun create(db: SQLiteDatabase, input: Input): Long {
        val valid = validated(input)
        db.beginTransaction()
        return try {
            requireActiveClass(db, valid.classroomId)
            require(!hasConflict(db, valid)) { "Já existe um compromisso nesse horário para a turma." }
            val id = db.insertOrThrow("appointments", null, values(valid))
            db.setTransactionSuccessful()
            id
        } finally { db.endTransaction() }
    }

    fun update(db: SQLiteDatabase, id: Long, input: Input) {
        val valid = validated(input)
        db.beginTransaction()
        try {
            requireActiveClass(db, valid.classroomId)
            db.rawQuery("SELECT 1 FROM appointments WHERE id=?", arrayOf(id.toString())).use { c ->
                require(c.moveToFirst()) { "Compromisso não encontrado." }
            }
            require(!hasConflict(db, valid, excludingId = id)) { "Já existe um compromisso nesse horário para a turma." }
            require(db.update("appointments", values(valid), "id=?", arrayOf(id.toString())) == 1) {
                "Compromisso não encontrado."
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
