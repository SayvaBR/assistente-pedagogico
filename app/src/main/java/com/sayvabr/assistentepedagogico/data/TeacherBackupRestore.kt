package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Restore a user-selected backup after preview and explicit confirmation. All inserts share one transaction.
 * Document bytes and SAF permission grants are never transferred by a JSON backup.
 */
object TeacherBackupRestore {
    data class Preview(val classrooms: Int, val students: Int, val lessons: Int,
        val attendance: Int, val observations: Int, val appointments: Int, val files: Int, val activities: Int = 0)

    private data class Payload(
        val profile: JSONObject?, val classrooms: List<JSONObject>, val students: List<JSONObject>,
        val lessons: List<JSONObject>, val attendance: List<JSONObject>, val observations: List<JSONObject>,
        val appointments: List<JSONObject>, val folders: List<JSONObject>, val files: List<JSONObject>,
        val activities: List<JSONObject>, val layouts: List<JSONObject>, val templates: List<JSONObject>,
    ) {
        fun preview() = Preview(classrooms.size, students.size, lessons.size, attendance.size,
            observations.size, appointments.size, files.size, activities.size)
    }

    private fun JSONArray.rows(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.required(key: String): String = getString(key).trim().also {
        require(it.isNotEmpty() && it != "null") { "Campo obrigatório inválido: $key." }
    }
    private fun JSONObject.optional(key: String, fallback: String = ""): String =
        if (isNull(key)) fallback else getString(key)
    private fun JSONObject.optionalId(key: String): Long? =
        if (isNull(key)) null else getLong(key).also { require(it > 0) { "Identificador inválido: $key." } }
    private fun JSONObject.id(key: String = "id"): Long = getLong(key).also {
        require(it > 0) { "Identificador inválido: $key." }
    }
    private fun JSONObject.day(key: String) = LocalDate.parse(required(key))
    private fun JSONObject.clock(key: String) = LocalTime.parse(required(key))
    private fun List<JSONObject>.uniqueIds(label: String, key: String = "id"): Set<Long> =
        map { it.id(key) }.also { require(it.size == it.toSet().size) { "$label contém IDs repetidos." } }.toSet()

    private fun parse(payload: String): Payload {
        val root = TeacherBackupCodec.validate(payload)
        val result = Payload(
            profile = root.optJSONObject("profile"),
            classrooms = root.getJSONArray("classrooms").rows(),
            students = root.getJSONArray("students").rows(),
            lessons = root.getJSONArray("lessons").rows(),
            attendance = root.getJSONArray("attendance").rows(),
            observations = root.getJSONArray("observations").rows(),
            appointments = root.getJSONArray("appointments").rows(),
            folders = root.getJSONArray("folders").rows(),
            files = root.getJSONArray("files").rows(),
            activities = root.getJSONArray("activities").rows(),
            layouts = root.getJSONArray("lessonLayouts").rows(),
            templates = root.getJSONArray("planTemplates").rows(),
        )
        result.profile?.required("name")
        val classes = result.classrooms.uniqueIds("Turmas")
        val students = result.students.uniqueIds("Alunos")
        val lessons = result.lessons.uniqueIds("Planos")
        result.activities.uniqueIds("Atividades")
        result.observations.uniqueIds("Observações")
        result.appointments.uniqueIds("Compromissos")
        val folders = result.folders.uniqueIds("Pastas")
        result.files.uniqueIds("Arquivos")
        result.templates.uniqueIds("Modelos")
        result.layouts.uniqueIds("Estruturas de plano", "lessonId")
        require(result.folders.map { it.required("name").lowercase() }.distinct().size == result.folders.size) {
            "Há pastas com nomes repetidos."
        }
        require(result.files.map { it.required("uri") }.distinct().size == result.files.size) {
            "Há arquivos com endereços repetidos."
        }
        result.classrooms.forEach { it.required("name"); it.required("stage"); it.required("shift") }
        result.students.forEach { require(it.id("classroomId") in classes) { "Aluno sem turma no backup." }; it.required("name") }
        result.lessons.forEach {
            require(it.id("classroomId") in classes) { "Plano sem turma no backup." }
            it.required("title"); it.required("subject"); it.day("date"); it.clock("time")
            it.required("objective"); it.required("content"); it.required("method")
            require(it.optInt("durationMinutes", 50) in 10..480) { "Duração de plano inválida." }
            listOf("openingMinutes", "developmentMinutes", "closingMinutes").forEach { key ->
                require(it.optInt(key, 0) >= 0) { "Tempo de aula inválido." }
            }
        }
        result.attendance.forEach {
            require(it.id("classroomId") in classes && it.id("studentId") in students) { "Frequência sem turma ou aluno." }
            it.day("date")
            require(it.required("status") in setOf("P", "F")) { "Frequência inválida." }
        }
        result.observations.forEach {
            require(it.id("classroomId") in classes) { "Observação sem turma." }
            require(it.optionalId("studentId")?.let { id -> id in students } != false) { "Observação sem aluno." }
            it.required("kind"); it.required("body"); it.day("date")
        }
        result.appointments.forEach {
            require(it.optionalId("classroomId")?.let { id -> id in classes } != false) { "Compromisso sem turma." }
            it.required("title"); it.day("date"); it.clock("time")
            if (!it.isNull("endTime")) it.clock("endTime")
        }
        result.folders.forEach { require(it.required("name").length <= 80 && !it.getString("name").contains('/')) { "Pasta inválida." } }
        result.files.forEach {
            require(it.required("uri").startsWith("content://")) { "Referência de arquivo inválida." }
            it.required("name")
            require(it.optionalId("folderId")?.let { id -> id in folders } != false) { "Arquivo sem pasta." }
        }
        // Validate ALL nested compositions and references before the destructive transaction.
        result.layouts.forEach { row ->
            require(row.id("lessonId") in lessons) { "Estrutura vinculada a plano inexistente." }
            PlanLayoutV9.decode(row.getJSONObject("layout").toString())
        }
        val names = mutableSetOf<String>()
        result.templates.forEach { row ->
            val template = PlanTemplate(row.required("name"), PlanLayoutV9.decode(row.getJSONObject("layout").toString()).blocks)
            require(names.add(template.name.lowercase())) { "Modelos com nomes repetidos." }
        }
        return result
    }

    fun preview(payload: String): Preview = parse(payload).preview()

    private fun values(vararg pairs: Pair<String, Any?>) = ContentValues().apply {
        pairs.forEach { (key, value) -> when (value) {
            null -> putNull(key)
            is Long -> put(key, value)
            is Int -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
            else -> put(key, value.toString())
        } }
    }

    fun restore(db: SQLiteDatabase, payload: String) {
        require(db.version == PlanLayoutV9.VERSION) { "Atualize o banco antes de restaurar." }
        // Validation must finish before the first DELETE; malformed input cannot touch current data.
        val records = parse(payload)
        db.beginTransaction()
        try {
            listOf("lesson_layouts", "plan_templates", "lesson_activities", "attendance", "observations", "lessons", "students", "appointments", "saved_files",
                "file_folders", "classrooms", "profile").forEach { db.delete(it, null, null) }
            records.profile?.let { db.insertOrThrow("profile", null, values("id" to 1, "name" to it.required("name"))) }
            records.classrooms.forEach { db.insertOrThrow("classrooms", null, values(
                "id" to it.id(), "name" to it.required("name"), "stage" to it.required("stage"),
                "shift" to it.required("shift"), "archived" to it.optBoolean("archived", false))) }
            records.students.forEach { db.insertOrThrow("students", null, values(
                "id" to it.id(), "classroom_id" to it.id("classroomId"), "name" to it.required("name"))) }
            records.lessons.forEach { lesson ->
                val legacyArchived = lesson.optBoolean("archived", false)
                val status = if (lesson.has("pedagogicalStatus")) LessonStatus.parse(lesson.getString("pedagogicalStatus"))
                    else if (legacyArchived) LessonStatus.ARCHIVED else LessonStatus.DRAFT
                val previous = if (lesson.has("statusBeforeArchive")) LessonStatus.parse(lesson.getString("statusBeforeArchive"))
                    else LessonStatus.DRAFT
                require(previous != LessonStatus.ARCHIVED && (status == LessonStatus.ARCHIVED) == legacyArchived) {
                    "Estado pedagógico inconsistente no backup."
                }
                db.insertOrThrow("lessons", null, values(
                    "id" to lesson.id(), "classroom_id" to lesson.id("classroomId"), "title" to lesson.required("title"),
                    "subject" to lesson.required("subject"), "day" to lesson.required("date"), "time" to lesson.required("time"),
                    "duration_minutes" to lesson.optInt("durationMinutes", 50), "objective" to lesson.required("objective"),
                    "specific_objectives" to lesson.optional("specificObjectives"), "content" to lesson.required("content"),
                    "bncc_codes" to lesson.optional("bnccCodes"), "justification" to lesson.optional("justification"),
                    "method" to lesson.required("method"), "opening" to lesson.optional("opening"),
                    "opening_minutes" to lesson.optInt("openingMinutes", 0), "development" to lesson.optional("development"),
                    "development_minutes" to lesson.optInt("developmentMinutes", 0), "closing" to lesson.optional("closing"),
                    "closing_minutes" to lesson.optInt("closingMinutes", 0), "assessment" to lesson.optional("assessment"),
                    "adaptations" to lesson.optional("adaptations"), "archived" to legacyArchived,
                    "pedagogical_status" to status.value, "status_before_archive" to previous.value))
            }
            records.layouts.forEach { row -> db.insertOrThrow("lesson_layouts", null, values(
                "lesson_id" to row.id("lessonId"),
                "layout_json" to PlanLayoutV9.encode(PlanLayoutV9.decode(row.getJSONObject("layout").toString())))) }
            records.templates.forEach { row -> db.insertOrThrow("plan_templates", null, values(
                "id" to row.id(), "name" to row.required("name"),
                "layout_json" to PlanLayoutV9.encode(PlanLayoutV9.decode(row.getJSONObject("layout").toString())))) }
            // Insert after classrooms and lessons: SQLite FKs and explicit class ownership are validated.
            records.activities.forEach { db.insertOrThrow("lesson_activities", null, values(
                "id" to it.id(), "classroom_id" to it.id("classroomId"),
                "lesson_id" to it.optionalId("lessonId"), "title" to it.required("title"),
                "instructions" to it.required("instructions"), "duration_minutes" to it.getInt("durationMinutes"))) }
            records.attendance.forEach { db.insertOrThrow("attendance", null, values(
                "classroom_id" to it.id("classroomId"), "student_id" to it.id("studentId"),
                "day" to it.required("date"), "status" to it.required("status"))) }
            records.observations.forEach { db.insertOrThrow("observations", null, values(
                "id" to it.id(), "classroom_id" to it.id("classroomId"), "student_id" to it.optionalId("studentId"),
                "kind" to it.required("kind"), "body" to it.required("body"), "day" to it.required("date"),
                "share_approved" to it.optBoolean("shareApproved", false))) }
            records.appointments.forEach { db.insertOrThrow("appointments", null, values(
                "id" to it.id(), "title" to it.required("title"), "day" to it.required("date"),
                "time" to it.required("time"), "end_time" to it.optional("endTime", it.required("time")),
                "type" to it.optional("type", "Outro"), "classroom_id" to it.optionalId("classroomId"))) }
            records.folders.forEach { db.insertOrThrow("file_folders", null, values(
                "id" to it.id(), "name" to it.required("name"), "created_at" to Instant.now().toString())) }
            records.files.forEach { db.insertOrThrow("saved_files", null, values(
                "id" to it.id(), "name" to it.required("name"), "uri" to it.required("uri"),
                "folder_id" to it.optionalId("folderId"), "favorite" to it.optBoolean("favorite", false),
                "trashed_at" to if (it.isNull("trashedAt")) null else it.getString("trashedAt"),
                // URI permission is specific to an installation, even if an exported flag says available.
                "access_state" to "revoked")) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}

/** Invoked only by a UI flow that has shown the preview and asked for confirmation. */
fun TeacherStore.restoreBackupAfterConfirmation(payload: String, confirmed: Boolean) {
    require(confirmed) { "A restauração exige confirmação explícita." }
    TeacherBackupRestore.restore(writableDatabase, payload)
}
