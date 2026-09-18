package com.sayvabr.assistentepedagogico.data

import org.json.JSONArray
import org.json.JSONObject

/** Versioned local-only JSON codec. Export via TeacherStore to preserve archival origins. */
object TeacherBackupCodec {
    const val FORMAT = "assistente-pedagogico-backup"
    // Optional keys extend the original v1 envelope without breaking legacy imports.
    const val VERSION = 1

    fun encode(
        snapshot: TeacherSnapshot,
        activities: List<LessonActivityV7.Activity> = emptyList(),
        archiveOrigins: Map<Long, LessonStatus> = emptyMap(),
    ): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("profile", snapshot.profile?.let { JSONObject().put("name", it.name) } ?: JSONObject.NULL)
        put("classrooms", JSONArray().apply { snapshot.classrooms.forEach { c -> put(JSONObject().apply {
            put("id", c.id); put("name", c.name); put("stage", c.stage); put("shift", c.shift); put("archived", c.archived)
        }) } })
        put("students", JSONArray().apply { snapshot.students.forEach { s -> put(JSONObject().apply {
            put("id", s.id); put("classroomId", s.classroomId); put("name", s.name)
        }) } })
        put("lessons", JSONArray().apply { snapshot.lessons.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("classroomId", l.classroomId); put("title", l.title); put("subject", l.subject)
            put("date", l.date); put("time", l.time); put("durationMinutes", l.durationMinutes)
            put("objective", l.objective); put("specificObjectives", l.specificObjectives); put("content", l.content)
            put("bnccCodes", l.bnccCodes); put("justification", l.justification); put("method", l.method)
            put("opening", l.opening); put("openingMinutes", l.openingMinutes)
            put("development", l.development); put("developmentMinutes", l.developmentMinutes)
            put("closing", l.closing); put("closingMinutes", l.closingMinutes)
            put("assessment", l.assessment); put("adaptations", l.adaptations); put("archived", l.archived)
            put("pedagogicalStatus", l.status.value)
            put("statusBeforeArchive", if (l.archived) archiveOrigins[l.id]?.value ?: LessonStatus.DRAFT.value else l.status.value)
        }) } })
        put("activities", JSONArray().apply { activities.forEach { activity -> put(JSONObject().apply {
            put("id", activity.id); put("classroomId", activity.classroomId)
            put("lessonId", activity.lessonId ?: JSONObject.NULL)
            put("title", activity.title); put("instructions", activity.instructions)
            put("durationMinutes", activity.durationMinutes)
        }) } })
        put("attendance", JSONArray().apply { snapshot.attendance.forEach { a -> put(JSONObject().apply {
            put("classroomId", a.classroomId); put("studentId", a.studentId); put("date", a.date); put("status", a.status)
        }) } })
        put("observations", JSONArray().apply { snapshot.observations.forEach { o -> put(JSONObject().apply {
            put("id", o.id); put("classroomId", o.classroomId); put("studentId", o.studentId ?: JSONObject.NULL)
            put("kind", o.kind); put("body", o.body); put("date", o.date); put("shareApproved", o.shareApproved)
        }) } })
        put("appointments", JSONArray().apply { snapshot.appointments.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("title", a.title); put("date", a.date); put("time", a.time)
            put("endTime", a.endTime); put("type", a.type)
            put("classroomId", a.classroomId ?: JSONObject.NULL)
        }) } })
        // SAF grants cannot be backed up. Restore always requires reauthorization.
        put("files", JSONArray().apply { snapshot.files.forEach { f -> put(JSONObject().apply {
            put("id", f.id); put("name", f.name); put("uri", f.uri); put("folderId", f.folderId ?: JSONObject.NULL)
            put("favorite", f.favorite); put("trashedAt", f.trashedAt ?: JSONObject.NULL); put("accessState", "revoked")
        }) } })
        put("folders", JSONArray().apply { snapshot.folders.forEach { f -> put(JSONObject().apply {
            put("id", f.id); put("name", f.name)
        }) } })
    }.toString()

    /** Validate envelope and ownership before preview or restore can touch SQLite. */
    fun validate(payload: String): JSONObject {
        require(payload.toByteArray(Charsets.UTF_8).size <= 10 * 1024 * 1024) { "Backup excede o limite de 10 MB." }
        val root = runCatching { JSONObject(payload) }.getOrElse { throw IllegalArgumentException("Backup inválido.", it) }
        require(root.optString("format") == FORMAT) { "Este arquivo não é um backup do Assistente Pedagógico." }
        require(root.optInt("version", -1) == VERSION) { "Versão de backup ainda não suportada." }
        if (!root.has("folders")) {
            val files = root.optJSONArray("files")
            require(files != null) { "Backup incompleto: files." }
            for (index in 0 until files.length()) {
                require(files.getJSONObject(index).isNull("folderId")) {
                    "Backup incompleto: há arquivos vinculados a pastas, mas falta o catálogo de pastas."
                }
            }
            root.put("folders", JSONArray())
        }
        if (!root.has("activities")) root.put("activities", JSONArray())
        listOf("classrooms", "students", "lessons", "activities", "attendance", "observations", "appointments", "files", "folders").forEach {
            require(root.optJSONArray(it) != null) { "Backup incompleto: $it." }
        }
        TeacherBackupIntegrity.validate(root)
        return root
    }
}
