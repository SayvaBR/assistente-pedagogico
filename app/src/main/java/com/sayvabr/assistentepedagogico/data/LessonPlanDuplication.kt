package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

/** A copy is an independent lesson with independent activity records, never a second reference to the original. */
object LessonPlanDuplication {
    /**
     * Copy a non-archived lesson belonging to an active classroom, preserving all pedagogical fields.
     * Activities linked to the original are copied and relinked to the new lesson in the same transaction.
     * A different date may be selected, but the original lesson and activities remain unchanged.
     */
    fun duplicate(db: SQLiteDatabase, classroomId: Long, lessonId: Long, targetDay: String? = null): Long {
        require(classroomId > 0 && lessonId > 0) { "Turma ou plano inválido." }
        val newDay = targetDay?.also { LocalDate.parse(it) }
        db.beginTransaction()
        return try {
            db.rawQuery("SELECT archived FROM classrooms WHERE id=?", arrayOf(classroomId.toString())).use { classroom ->
                require(classroom.moveToFirst() && classroom.getInt(0) == 0) { "Turma não encontrada ou arquivada." }
            }
            val values = db.query(
                "lessons", null, "id=? AND classroom_id=? AND archived=0",
                arrayOf(lessonId.toString(), classroomId.toString()), null, null, null,
            ).use { source ->
                require(source.moveToFirst()) { "Plano não encontrado nesta turma ou está arquivado." }
                ContentValues().apply {
                    source.columnNames.forEachIndexed { index, name ->
                        if (name == "id") return@forEachIndexed
                        when (source.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> putNull(name)
                            Cursor.FIELD_TYPE_INTEGER -> put(name, source.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> put(name, source.getDouble(index))
                            Cursor.FIELD_TYPE_STRING -> put(name, source.getString(index))
                            Cursor.FIELD_TYPE_BLOB -> put(name, source.getBlob(index))
                        }
                    }
                    put("title", "${source.getString(source.getColumnIndexOrThrow("title"))} (cópia)")
                    if (newDay != null) put("day", newDay)
                    put("archived", 0)
                }
            }
            val newId = db.insertOrThrow("lessons", null, values)
            require(newId > 0) { "Não foi possível duplicar o plano." }
            db.execSQL(
                """INSERT INTO lesson_activities (classroom_id,lesson_id,title,instructions,duration_minutes)
                   SELECT classroom_id,?,title,instructions,duration_minutes FROM lesson_activities
                   WHERE classroom_id=? AND lesson_id=?""".trimIndent(),
                arrayOf(newId, classroomId, lessonId),
            )
            db.setTransactionSuccessful()
            newId
        } finally { db.endTransaction() }
    }
}

/** Public entry point through the same Activity-owned SQLite helper used throughout the application. */
fun TeacherStore.duplicateLesson(classroomId: Long, lessonId: Long, targetDay: String? = null): Long =
    LessonPlanDuplication.duplicate(writableDatabase, classroomId, lessonId, targetDay)
