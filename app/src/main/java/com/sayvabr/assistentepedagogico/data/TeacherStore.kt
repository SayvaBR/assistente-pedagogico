package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.LocalTime

/** All records stay in the app's private SQLite database. Android automatic backup is disabled. */
data class TeacherProfile(val name: String)
data class Classroom(val id: Long, val name: String, val stage: String, val shift: String, val archived: Boolean = false)
data class Student(val id: Long, val classroomId: Long, val name: String)
data class Lesson(val id: Long, val classroomId: Long, val title: String, val subject: String, val date: String, val time: String, val objective: String, val content: String, val method: String, val archived: Boolean = false)
data class Attendance(val classroomId: Long, val studentId: Long, val date: String, val status: String)
data class Observation(val id: Long, val classroomId: Long, val studentId: Long?, val kind: String, val body: String, val date: String, val shareApproved: Boolean)
data class Appointment(val id: Long, val title: String, val date: String, val time: String)
data class SavedFile(
    val id: Long, val name: String, val uri: String,
    val folderId: Long? = null, val favorite: Boolean = false,
    val trashedAt: String? = null, val accessState: String = "unknown",
)
data class TeacherSnapshot(
    val profile: TeacherProfile?,
    val classrooms: List<Classroom>,
    val students: List<Student>,
    val lessons: List<Lesson>,
    val attendance: List<Attendance>,
    val observations: List<Observation>,
    val appointments: List<Appointment>,
    val files: List<SavedFile>,
    val folders: List<FileFolder> = emptyList(),
)

class TeacherStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "pedagogico.db", null, FileLibraryV4.VERSION) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        listOf(
            "CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL)",
            "CREATE TABLE classrooms (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, stage TEXT NOT NULL, shift TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE students (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, name TEXT NOT NULL)",
            "CREATE TABLE lessons (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, title TEXT NOT NULL, subject TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL, objective TEXT NOT NULL, content TEXT NOT NULL, method TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE attendance (classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE, day TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('P','F')), PRIMARY KEY(student_id,day))",
            "CREATE TABLE observations (id INTEGER PRIMARY KEY AUTOINCREMENT, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE, student_id INTEGER REFERENCES students(id) ON DELETE SET NULL, kind TEXT NOT NULL, body TEXT NOT NULL, day TEXT NOT NULL, share_approved INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL)",
            "CREATE TABLE saved_files (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, uri TEXT NOT NULL UNIQUE)",
        ).forEach(db::execSQL)
        FileLibraryV4.migrate(db)
    }

    /** SQLiteOpenHelper wraps onUpgrade in a transaction, rolling back every ALTER on failure. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..3 && newVersion == FileLibraryV4.VERSION) {
            "Unsupported database migration from $oldVersion to $newVersion"
        }
        if (oldVersion < 2) db.execSQL("ALTER TABLE classrooms ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 3) db.execSQL("ALTER TABLE lessons ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 4) FileLibraryV4.migrate(db)
    }

    fun read(): TeacherSnapshot {
        val db = readableDatabase
        var profile: TeacherProfile? = null
        db.rawQuery("SELECT name FROM profile WHERE id=1", null).use { c -> if (c.moveToFirst()) profile = TeacherProfile(c.getString(0)) }
        val classrooms = mutableListOf<Classroom>()
        db.rawQuery("SELECT id,name,stage,shift,archived FROM classrooms ORDER BY id DESC", null).use { c -> while (c.moveToNext()) classrooms += Classroom(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4) == 1) }
        val students = mutableListOf<Student>()
        db.rawQuery("SELECT id,classroom_id,name FROM students ORDER BY name COLLATE NOCASE", null).use { c -> while (c.moveToNext()) students += Student(c.getLong(0), c.getLong(1), c.getString(2)) }
        val lessons = mutableListOf<Lesson>()
        db.rawQuery("SELECT id,classroom_id,title,subject,day,time,objective,content,method,archived FROM lessons ORDER BY day,time", null).use { c -> while (c.moveToNext()) lessons += Lesson(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9) == 1) }
        val attendance = mutableListOf<Attendance>()
        db.rawQuery("SELECT classroom_id,student_id,day,status FROM attendance", null).use { c -> while (c.moveToNext()) attendance += Attendance(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3)) }
        val observations = mutableListOf<Observation>()
        db.rawQuery("SELECT id,classroom_id,student_id,kind,body,day,share_approved FROM observations ORDER BY id DESC", null).use { c -> while (c.moveToNext()) observations += Observation(c.getLong(0), c.getLong(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6) == 1) }
        val appointments = mutableListOf<Appointment>()
        db.rawQuery("SELECT id,title,day,time FROM appointments ORDER BY day,time", null).use { c -> while (c.moveToNext()) appointments += Appointment(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)) }
        val files = FileLibraryV4.files(db).filter { it.trashedAt == null }.map {
            SavedFile(it.id, it.name, it.uri, it.folderId, it.favorite, it.trashedAt, it.accessState)
        }
        return TeacherSnapshot(profile, classrooms, students, lessons, attendance, observations, appointments, files, FileLibraryV4.folders(db))
    }

    /** This is the only source of trashed catalog references; normal snapshots hide the trash. */
    fun libraryFiles(): List<LibraryFile> = FileLibraryV4.files(readableDatabase)
    fun fileFolders(): List<FileFolder> = FileLibraryV4.folders(readableDatabase)
    fun createFolder(name: String): Long = FileLibraryV4.createFolder(writableDatabase, name)
    fun renameFolder(folderId: Long, name: String) = FileLibraryV4.renameFolder(writableDatabase, folderId, name)
    fun deleteFolder(folderId: Long) = FileLibraryV4.deleteFolder(writableDatabase, folderId)
    fun moveFile(fileId: Long, folderId: Long?) = FileLibraryV4.moveFile(writableDatabase, fileId, folderId)
    fun setFileFavorite(fileId: Long, favorite: Boolean) = FileLibraryV4.setFavorite(writableDatabase, fileId, favorite)
    fun trashFile(fileId: Long) = FileLibraryV4.trash(writableDatabase, fileId)
    fun restoreFile(fileId: Long) = FileLibraryV4.restore(writableDatabase, fileId)
    fun permanentlyRemoveFileReference(fileId: Long) = FileLibraryV4.permanentlyRemoveReference(writableDatabase, fileId)
    fun setFileAccessState(fileId: Long, state: String) = FileLibraryV4.setAccessState(writableDatabase, fileId, state)

    private fun values(vararg pairs: Pair<String, Any?>) = ContentValues().apply {
        pairs.forEach { (key, value) -> when (value) {
            null -> putNull(key)
            is Long -> put(key, value)
            is Int -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
            else -> put(key, value.toString())
        } }
    }

    fun saveProfile(name: String) {
        require(name.trim().length >= 2) { "Informe seu nome (ao menos 2 caracteres)." }
        writableDatabase.insertWithOnConflict("profile", null, values("id" to 1, "name" to name.trim()), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun createClass(name: String, stage: String, shift: String): Long {
        require(name.isNotBlank()) { "Informe o nome da turma." }
        require(stage in listOf("Educação Infantil", "Ensino Fundamental", "Ensino Médio")) { "Selecione a etapa de ensino." }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            db.rawQuery("SELECT COUNT(*) FROM classrooms WHERE archived=0", null).use { c -> c.moveToFirst(); require(c.getInt(0) < 2) { "O plano gratuito permite 2 turmas. Seus dados continuam preservados." } }
            val id = db.insertOrThrow("classrooms", null, values("name" to name.trim(), "stage" to stage, "shift" to shift))
            db.setTransactionSuccessful()
            id
        } finally { db.endTransaction() }
    }

    fun updateClass(classroomId: Long, name: String, stage: String, shift: String) {
        require(name.trim().isNotEmpty()) { "Informe o nome da turma." }
        require(stage in listOf("Educação Infantil", "Ensino Fundamental", "Ensino Médio")) { "Selecione a etapa de ensino." }
        val changed = writableDatabase.update("classrooms", values("name" to name.trim(), "stage" to stage, "shift" to shift), "id=? AND archived=0", arrayOf(classroomId.toString()))
        require(changed == 1) { "Turma não encontrada ou arquivada." }
    }

    fun setClassArchived(classroomId: Long, archived: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT archived FROM classrooms WHERE id=?", arrayOf(classroomId.toString())).use { c ->
                require(c.moveToFirst()) { "Turma não encontrada." }
                if ((c.getInt(0) == 1) == archived) { db.setTransactionSuccessful(); return }
            }
            if (!archived) {
                db.rawQuery("SELECT COUNT(*) FROM classrooms WHERE archived=0", null).use { c ->
                    c.moveToFirst()
                    require(c.getInt(0) < 2) { "O plano gratuito permite até 2 turmas ativas. Arquive outra turma para restaurar esta." }
                }
            }
            require(db.update("classrooms", values("archived" to archived), "id=?", arrayOf(classroomId.toString())) == 1)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun updateStudent(classroomId: Long, studentId: Long, name: String) {
        require(name.trim().length >= 2) { "Informe o nome do aluno." }
        val changed = writableDatabase.update("students", values("name" to name.trim()), "id=? AND classroom_id=?", arrayOf(studentId.toString(), classroomId.toString()))
        require(changed == 1) { "Aluno não encontrado nesta turma." }
    }

    fun deleteStudent(classroomId: Long, studentId: Long) {
        val removed = writableDatabase.delete("students", "id=? AND classroom_id=?", arrayOf(studentId.toString(), classroomId.toString()))
        require(removed == 1) { "Aluno não encontrado nesta turma." }
    }

    fun addStudent(classroomId: Long, name: String) {
        require(name.trim().length >= 2) { "Informe o nome do aluno." }
        writableDatabase.insertOrThrow("students", null, values("classroom_id" to classroomId, "name" to name.trim()))
    }

    fun saveLesson(classroomId: Long, title: String, subject: String, date: String, time: String, objective: String, content: String, method: String) {
        require(title.isNotBlank() && subject.isNotBlank() && objective.isNotBlank()) { "Preencha o título, a disciplina e o objetivo." }
        LocalDate.parse(date)
        LocalTime.parse(time)
        writableDatabase.insertOrThrow("lessons", null, values("classroom_id" to classroomId, "title" to title.trim(), "subject" to subject.trim(), "day" to date, "time" to time, "objective" to objective.trim(), "content" to content.trim(), "method" to method.trim()))
    }

    /** Edits only a plan belonging to the selected classroom; keeps its ID and other records untouched. */
    fun updateLesson(classroomId: Long, lessonId: Long, title: String, subject: String, date: String, time: String, objective: String, content: String, method: String) {
        require(title.isNotBlank() && subject.isNotBlank() && objective.isNotBlank()) { "Preencha o título, a disciplina e o objetivo." }
        LocalDate.parse(date)
        LocalTime.parse(time)
        val changed = writableDatabase.update("lessons", values("title" to title.trim(), "subject" to subject.trim(), "day" to date, "time" to time, "objective" to objective.trim(), "content" to content.trim(), "method" to method.trim()), "id=? AND classroom_id=? AND archived=0", arrayOf(lessonId.toString(), classroomId.toString()))
        require(changed == 1) { "Plano não encontrado nesta turma ou está arquivado." }
    }

    /** Reversible archival preserves the lesson, classroom and all historical data. */
    fun setLessonArchived(classroomId: Long, lessonId: Long, archived: Boolean) {
        val changed = writableDatabase.update("lessons", values("archived" to archived), "id=? AND classroom_id=?", arrayOf(lessonId.toString(), classroomId.toString()))
        require(changed == 1) { "Plano não encontrado nesta turma." }
    }

    fun saveAttendance(classroomId: Long, day: String, marks: Map<Long, String>) {
        LocalDate.parse(day)
        val db = writableDatabase
        db.beginTransaction()
        try {
            val allowed = mutableSetOf<Long>()
            db.rawQuery("SELECT id FROM students WHERE classroom_id=?", arrayOf(classroomId.toString())).use { c -> while (c.moveToNext()) allowed += c.getLong(0) }
            require(marks.keys.all { it in allowed }) { "Aluno não pertence a esta turma." }
            marks.forEach { (studentId, status) ->
                if (status == "?") db.delete("attendance", "student_id=? AND day=?", arrayOf(studentId.toString(), day))
                else {
                    require(status == "P" || status == "F") { "Situação de frequência inválida." }
                    db.insertWithOnConflict("attendance", null, values("classroom_id" to classroomId, "student_id" to studentId, "day" to day, "status" to status), SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun addObservation(classroomId: Long, studentId: Long?, kind: String, body: String, shareApproved: Boolean) {
        require(body.trim().length >= 5) { "Descreva a observação com ao menos 5 caracteres." }
        require(kind in listOf("Comportamento", "Participação", "Aprendizagem", "Outro")) { "Tipo de observação inválido." }
        if (studentId != null) {
            readableDatabase.rawQuery("SELECT 1 FROM students WHERE id=? AND classroom_id=?", arrayOf(studentId.toString(), classroomId.toString())).use { cursor ->
                require(cursor.moveToFirst()) { "Aluno não pertence a esta turma." }
            }
        }
        writableDatabase.insertOrThrow("observations", null, values("classroom_id" to classroomId, "student_id" to studentId, "kind" to kind, "body" to body.trim(), "day" to LocalDate.now().toString(), "share_approved" to shareApproved))
    }

    /** Edits an existing pedagogical observation without changing its identity or original date. */
    fun updateObservation(classroomId: Long, observationId: Long, studentId: Long?, kind: String, body: String, shareApproved: Boolean) {
        require(body.trim().length >= 5) { "Descreva a observação com ao menos 5 caracteres." }
        require(kind in listOf("Comportamento", "Participação", "Aprendizagem", "Outro")) { "Tipo de observação inválido." }
        if (studentId != null) {
            readableDatabase.rawQuery("SELECT 1 FROM students WHERE id=? AND classroom_id=?", arrayOf(studentId.toString(), classroomId.toString())).use { c ->
                require(c.moveToFirst()) { "Aluno não pertence a esta turma." }
            }
        }
        val changed = writableDatabase.update("observations", values("student_id" to studentId, "kind" to kind, "body" to body.trim(), "share_approved" to shareApproved), "id=? AND classroom_id=?", arrayOf(observationId.toString(), classroomId.toString()))
        require(changed == 1) { "Observação não encontrada nesta turma." }
    }

    /** Permanently deletes only the selected observation. */
    fun deleteObservation(classroomId: Long, observationId: Long) {
        val removed = writableDatabase.delete("observations", "id=? AND classroom_id=?", arrayOf(observationId.toString(), classroomId.toString()))
        require(removed == 1) { "Observação não encontrada nesta turma." }
    }

    fun addAppointment(title: String, day: String, time: String) {
        require(title.isNotBlank()) { "Informe o compromisso." }
        LocalDate.parse(day)
        LocalTime.parse(time)
        writableDatabase.insertOrThrow("appointments", null, values("title" to title.trim(), "day" to day, "time" to time))
    }

    /** Keeps the same record ID when changing a commitment, including its date. */
    fun updateAppointment(appointmentId: Long, title: String, day: String, time: String) {
        require(title.trim().isNotEmpty()) { "Informe o compromisso." }
        LocalDate.parse(day)
        LocalTime.parse(time)
        val changed = writableDatabase.update("appointments", values("title" to title.trim(), "day" to day, "time" to time), "id=?", arrayOf(appointmentId.toString()))
        require(changed == 1) { "Compromisso não encontrado." }
    }

    /** Deletes only the selected commitment; no other schedule records are affected. */
    fun deleteAppointment(appointmentId: Long) {
        val removed = writableDatabase.delete("appointments", "id=?", arrayOf(appointmentId.toString()))
        require(removed == 1) { "Compromisso não encontrado." }
    }

    /** Renames the app catalog entry only; the original SAF document is never modified. */
    fun renameFile(fileId: Long, name: String) {
        require(name.trim().isNotEmpty() && name.trim().length <= 180) { "Informe um nome de arquivo válido (até 180 caracteres)." }
        val updated = writableDatabase.update("saved_files", values("name" to name.trim()), "id=? AND trashed_at IS NULL", arrayOf(fileId.toString()))
        require(updated == 1) { "Arquivo não encontrado ou está na lixeira." }
    }

    /** Former remove action is now reversible; the actual SAF source is never deleted. */
    fun removeFile(fileId: Long) = trashFile(fileId)

    /** Re-selecting a URI revives trashed references and confirms access without losing metadata. */
    fun addFile(name: String, uri: String): Long = FileImportPolicy.importReference(writableDatabase, name, uri)
}
