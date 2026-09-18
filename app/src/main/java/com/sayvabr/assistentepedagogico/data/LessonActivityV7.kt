package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * v7 persistence for reusable activities owned by a classroom and optionally linked to a lesson.
 * The database enforces ownership consistency: a lesson from another classroom cannot be linked.
 */
object LessonActivityV7 {
    const val VERSION = 7

    data class Activity(
        val id: Long,
        val classroomId: Long,
        val lessonId: Long?,
        val title: String,
        val instructions: String,
        val durationMinutes: Int,
    )

    data class Input(
        val classroomId: Long,
        val lessonId: Long? = null,
        val title: String,
        val instructions: String,
        val durationMinutes: Int,
    )

    fun validated(input: Input): Input {
        require(input.classroomId > 0) { "Selecione uma turma válida." }
        require(input.lessonId == null || input.lessonId > 0) { "Plano de aula inválido." }
        val title = input.title.trim()
        val instructions = input.instructions.trim()
        require(title.length in 3..120) { "Informe um título de atividade entre 3 e 120 caracteres." }
        require(instructions.length in 5..4000) { "Descreva a atividade com pelo menos 5 caracteres." }
        require(input.durationMinutes in 5..480) { "A duração da atividade deve ficar entre 5 minutos e 8 horas." }
        return input.copy(title = title, instructions = instructions)
    }

    fun migrate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS lesson_activities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
                lesson_id INTEGER REFERENCES lessons(id) ON DELETE SET NULL,
                title TEXT NOT NULL,
                instructions TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL CHECK(duration_minutes BETWEEN 5 AND 480)
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lesson_activities_classroom ON lesson_activities(classroom_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_lesson_activities_lesson ON lesson_activities(lesson_id)")
    }

    private fun assertOwnership(db: SQLiteDatabase, input: Input) {
        db.rawQuery("SELECT archived FROM classrooms WHERE id=?", arrayOf(input.classroomId.toString())).use {
            require(it.moveToFirst() && it.getInt(0) == 0) { "Turma não encontrada ou arquivada." }
        }
        input.lessonId?.let { lessonId ->
            db.rawQuery(
                "SELECT archived FROM lessons WHERE id=? AND classroom_id=?",
                arrayOf(lessonId.toString(), input.classroomId.toString()),
            ).use {
                require(it.moveToFirst() && it.getInt(0) == 0) {
                    "O plano não pertence à turma selecionada ou está arquivado."
                }
            }
        }
    }

    private fun values(value: Input) = ContentValues().apply {
        put("classroom_id", value.classroomId)
        if (value.lessonId == null) putNull("lesson_id") else put("lesson_id", value.lessonId)
        put("title", value.title)
        put("instructions", value.instructions)
        put("duration_minutes", value.durationMinutes)
    }

    fun create(db: SQLiteDatabase, input: Input): Long {
        val value = validated(input)
        assertOwnership(db, value)
        val id = db.insertOrThrow("lesson_activities", null, values(value))
        require(id > 0) { "Não foi possível salvar a atividade." }
        return id
    }

    fun list(db: SQLiteDatabase, classroomId: Long, lessonId: Long? = null): List<Activity> {
        require(classroomId > 0) { "Turma inválida." }
        require(lessonId == null || lessonId > 0) { "Plano inválido." }
        val result = mutableListOf<Activity>()
        val where = if (lessonId == null) "classroom_id=?" else "classroom_id=? AND lesson_id=?"
        val args = if (lessonId == null) arrayOf(classroomId.toString()) else arrayOf(classroomId.toString(), lessonId.toString())
        db.query("lesson_activities", arrayOf("id", "classroom_id", "lesson_id", "title", "instructions", "duration_minutes"), where, args, null, null, "id DESC").use { c ->
            while (c.moveToNext()) result += Activity(
                id = c.getLong(0), classroomId = c.getLong(1), lessonId = if (c.isNull(2)) null else c.getLong(2),
                title = c.getString(3), instructions = c.getString(4), durationMinutes = c.getInt(5),
            )
        }
        return result
    }

    fun update(db: SQLiteDatabase, activityId: Long, input: Input) {
        require(activityId > 0) { "Atividade inválida." }
        val value = validated(input)
        assertOwnership(db, value)
        require(db.update("lesson_activities", values(value), "id=? AND classroom_id=?", arrayOf(activityId.toString(), value.classroomId.toString())) == 1) {
            "Atividade não encontrada nesta turma."
        }
    }

    /** Makes an independent copy without changing the original, its classroom or its lesson link.
     * An optional target lesson must belong to the same active classroom.
     * The lookup and insert are atomic, so failure cannot leave a partial activity behind.
     */
    fun duplicate(db: SQLiteDatabase, classroomId: Long, activityId: Long, targetLessonId: Long? = null): Long {
        require(classroomId > 0 && activityId > 0) { "Atividade ou turma inválida." }
        db.beginTransaction()
        return try {
            val original = db.rawQuery(
                "SELECT lesson_id,title,instructions,duration_minutes FROM lesson_activities WHERE id=? AND classroom_id=?",
                arrayOf(activityId.toString(), classroomId.toString()),
            ).use { c ->
                require(c.moveToFirst()) { "Atividade não encontrada nesta turma." }
                Input(
                    classroomId = classroomId,
                    lessonId = if (c.isNull(0)) null else c.getLong(0),
                    title = c.getString(1), instructions = c.getString(2), durationMinutes = c.getInt(3),
                )
            }
            val copy = validated(original.copy(lessonId = targetLessonId ?: original.lessonId))
            assertOwnership(db, copy)
            val copiedId = db.insertOrThrow("lesson_activities", null, values(copy))
            require(copiedId > 0) { "Não foi possível duplicar a atividade." }
            db.setTransactionSuccessful()
            copiedId
        } finally { db.endTransaction() }
    }

    fun delete(db: SQLiteDatabase, classroomId: Long, activityId: Long) {
        require(classroomId > 0 && activityId > 0) { "Atividade ou turma inválida." }
        require(db.delete("lesson_activities", "id=? AND classroom_id=?", arrayOf(activityId.toString(), classroomId.toString())) == 1) {
            "Atividade não encontrada nesta turma."
        }
    }
}
