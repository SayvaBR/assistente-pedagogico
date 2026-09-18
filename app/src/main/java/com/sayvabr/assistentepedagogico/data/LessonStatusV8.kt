package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Status is an explicit persisted pedagogical decision, independent of the lesson's date. */
enum class LessonStatus(val value: String, val label: String) {
    DRAFT("draft", "Rascunho"),
    READY("ready", "Pronto"),
    COMPLETED("completed", "Concluído"),
    ARCHIVED("archived", "Arquivado");

    companion object {
        fun parse(value: String): LessonStatus = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Estado de plano inválido: $value.")
    }
}

/** Additive v7 -> v8 migration and atomic transitions. Never deletes or recreates lesson rows. */
object LessonStatusV8 {
    const val VERSION = 8

    fun migrate(db: SQLiteDatabase) {
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(lessons)", null).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(name)
        }
        if ("pedagogical_status" !in columns) {
            db.execSQL("ALTER TABLE lessons ADD COLUMN pedagogical_status TEXT NOT NULL DEFAULT 'draft' CHECK(pedagogical_status IN ('draft','ready','completed','archived'))")
        }
        if ("status_before_archive" !in columns) {
            db.execSQL("ALTER TABLE lessons ADD COLUMN status_before_archive TEXT NOT NULL DEFAULT 'draft' CHECK(status_before_archive IN ('draft','ready','completed'))")
        }
        // An old archived flag represents a real archived record, never an active draft.
        db.execSQL("UPDATE lessons SET pedagogical_status='archived' WHERE archived=1 AND pedagogical_status='draft'")
        db.rawQuery("SELECT pedagogical_status,status_before_archive,archived FROM lessons", null).use { cursor ->
            while (cursor.moveToNext()) {
                val status = LessonStatus.parse(cursor.getString(0))
                val previous = LessonStatus.parse(cursor.getString(1))
                require(previous != LessonStatus.ARCHIVED && (cursor.getInt(2) == 1) == (status == LessonStatus.ARCHIVED)) {
                    "Inconsistência nos estados pedagógicos do banco."
                }
            }
        }
    }

    /** The complete lesson moments must exist before a draft can be marked ready. */
    private fun assertReady(db: SQLiteDatabase, classroomId: Long, lessonId: Long) {
        db.rawQuery(
            """SELECT title,subject,objective,content,method,duration_minutes,
                opening,opening_minutes,development,development_minutes,closing,closing_minutes
                FROM lessons WHERE id=? AND classroom_id=?""".trimIndent(),
            arrayOf(lessonId.toString(), classroomId.toString()),
        ).use { c ->
            require(c.moveToFirst()) { "Plano não encontrado nesta turma." }
            require((0..4).all { c.getString(it).isNotBlank() }) { "Preencha os campos pedagógicos antes de marcar como pronto." }
            val duration = c.getLong(5)
            val minutes = listOf(7, 9, 11).map { c.getLong(it) }
            require(duration in 10L..480L && minutes.all { it > 0 && it <= duration } && minutes.sum() <= duration &&
                listOf(6, 8, 10).all { c.getString(it).isNotBlank() }) {
                "Preencha abertura, desenvolvimento e fechamento com tempo válido antes de marcar como pronto."
            }
        }
    }

    /** Every transition validates ownership, classroom activity, prior state and archival consistency. */
    fun transition(db: SQLiteDatabase, classroomId: Long, lessonId: Long, target: LessonStatus): LessonStatus {
        require(classroomId > 0 && lessonId > 0) { "Turma ou plano inválido." }
        db.beginTransaction()
        return try {
            db.rawQuery("SELECT archived FROM classrooms WHERE id=?", arrayOf(classroomId.toString())).use { classroom ->
                require(classroom.moveToFirst() && classroom.getInt(0) == 0) { "Turma não encontrada ou arquivada." }
            }
            val (current, previous, archived) = db.rawQuery(
                "SELECT pedagogical_status,status_before_archive,archived FROM lessons WHERE id=? AND classroom_id=?",
                arrayOf(lessonId.toString(), classroomId.toString()),
            ).use { c ->
                require(c.moveToFirst()) { "Plano não encontrado nesta turma." }
                Triple(LessonStatus.parse(c.getString(0)), LessonStatus.parse(c.getString(1)), c.getInt(2) == 1)
            }
            require(archived == (current == LessonStatus.ARCHIVED) && previous != LessonStatus.ARCHIVED) {
                "O estado do plano está inconsistente. Nenhum dado foi alterado."
            }
            require(current != target) { "O plano já está neste estado." }
            val allowed = when (current) {
                LessonStatus.DRAFT -> target == LessonStatus.READY || target == LessonStatus.ARCHIVED
                LessonStatus.READY -> target in setOf(LessonStatus.DRAFT, LessonStatus.COMPLETED, LessonStatus.ARCHIVED)
                LessonStatus.COMPLETED -> target == LessonStatus.READY || target == LessonStatus.ARCHIVED
                LessonStatus.ARCHIVED -> target == previous // Restore exactly the pre-archive state.
            }
            require(allowed) { "Transição de ${current.label} para ${target.label} não permitida." }
            if (target == LessonStatus.READY || target == LessonStatus.COMPLETED) assertReady(db, classroomId, lessonId)
            val values = ContentValues().apply {
                put("pedagogical_status", target.value)
                put("archived", if (target == LessonStatus.ARCHIVED) 1 else 0)
                if (target == LessonStatus.ARCHIVED) put("status_before_archive", current.value)
            }
            require(db.update("lessons", values, "id=? AND classroom_id=? AND pedagogical_status=? AND archived=?",
                arrayOf(lessonId.toString(), classroomId.toString(), current.value, if (archived) "1" else "0")) == 1) {
                "O plano foi modificado. Atualize e tente novamente."
            }
            db.setTransactionSuccessful()
            target
        } finally { db.endTransaction() }
    }

    fun restore(db: SQLiteDatabase, classroomId: Long, lessonId: Long): LessonStatus {
        val previous = db.rawQuery(
            "SELECT status_before_archive FROM lessons WHERE id=? AND classroom_id=? AND pedagogical_status='archived'",
            arrayOf(lessonId.toString(), classroomId.toString()),
        ).use { c ->
            require(c.moveToFirst()) { "Plano arquivado não encontrado nesta turma." }
            LessonStatus.parse(c.getString(0)).also { require(it != LessonStatus.ARCHIVED) { "Estado anterior inválido." } }
        }
        return transition(db, classroomId, lessonId, previous)
    }
}

/** Routes all status mutations through the Activity-owned SQLite helper. */
fun TeacherStore.transitionLessonStatus(classroomId: Long, lessonId: Long, target: LessonStatus): LessonStatus =
    LessonStatusV8.transition(writableDatabase, classroomId, lessonId, target)

fun TeacherStore.restoreLessonStatus(classroomId: Long, lessonId: Long): LessonStatus =
    LessonStatusV8.restore(writableDatabase, classroomId, lessonId)
