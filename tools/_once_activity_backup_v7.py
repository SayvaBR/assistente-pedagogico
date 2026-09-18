#!/usr/bin/env python3
"""One-shot fail-closed v7 backup migration. Removed by its workflow after successful commit."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
data = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/data'
paths = {name: data / name for name in (
    'TeacherBackupCodec.kt', 'TeacherBackupExport.kt', 'TeacherBackupIntegrity.kt', 'TeacherBackupRestore.kt',
)}
contents = {name: path.read_text(encoding='utf-8') for name, path in paths.items()}

def change(name: str, old: str, new: str):
    original = contents[name]
    count = original.count(old)
    if count != 1:
        raise SystemExit(f'ABORT {name}: expected exactly one anchor, got {count}: {old[:110]!r}')
    contents[name] = original.replace(old, new, 1)

codec = 'TeacherBackupCodec.kt'
change(codec, 'fun encode(snapshot: TeacherSnapshot): String = JSONObject().apply {',
       'fun encode(snapshot: TeacherSnapshot, activities: List<LessonActivityV7.Activity> = emptyList()): String = JSONObject().apply {')
change(codec, '        put("attendance", JSONArray().apply { snapshot.attendance.forEach { a -> put(JSONObject().apply {',
'''        put("activities", JSONArray().apply { activities.forEach { activity -> put(JSONObject().apply {
            put("id", activity.id); put("classroomId", activity.classroomId)
            put("lessonId", activity.lessonId ?: JSONObject.NULL)
            put("title", activity.title); put("instructions", activity.instructions)
            put("durationMinutes", activity.durationMinutes)
        }) } })
        put("attendance", JSONArray().apply { snapshot.attendance.forEach { a -> put(JSONObject().apply {''')
change(codec, '        listOf("classrooms", "students", "lessons", "attendance", "observations", "appointments", "files", "folders").forEach {',
'''        // Backups created before activity support legitimately have no activities list.
        // A present but non-array field is corrupt and must be rejected, never ignored.
        if (!root.has("activities")) root.put("activities", JSONArray())
        listOf("classrooms", "students", "lessons", "activities", "attendance", "observations", "appointments", "files", "folders").forEach {''')

export = 'TeacherBackupExport.kt'
change(export, '    return TeacherBackupCodec.encode(snapshot.copy(files = fullCatalog))',
'''    val activities = snapshot.classrooms.flatMap { listActivities(it.id) }
    return TeacherBackupCodec.encode(snapshot.copy(files = fullCatalog), activities)''')

integrity = 'TeacherBackupIntegrity.kt'
change(integrity, '        // Older backups legitimately do not know an appointment\'s duration (end == start).',
'''        // Reject wrong ownership and malformed activities BEFORE the restore transaction.
        val knownClassrooms = mutableSetOf<Long>()
        root.getJSONArray("classrooms").let { rooms ->
            for (index in 0 until rooms.length()) {
                require(knownClassrooms.add(rooms.getJSONObject(index).getLong("id"))) {
                    "Backup contém turmas com IDs duplicados."
                }
            }
        }
        val lessonOwners = mutableMapOf<Long, Long>()
        for (index in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(index)
            require(lessonOwners.put(lesson.getLong("id"), lesson.getLong("classroomId")) == null) {
                "Backup contém planos com IDs duplicados."
            }
        }
        val activities = root.getJSONArray("activities")
        val activityIds = mutableSetOf<Long>()
        for (index in 0 until activities.length()) {
            val activity = activities.getJSONObject(index)
            val id = activity.getLong("id")
            val classroomId = activity.getLong("classroomId")
            val lessonId = if (activity.isNull("lessonId")) null else activity.getLong("lessonId")
            require(id > 0 && activityIds.add(id)) { "Backup contém atividades com IDs inválidos ou duplicados." }
            require(classroomId in knownClassrooms) { "Atividade vinculada a turma inexistente no backup." }
            require(lessonId == null || lessonOwners[lessonId] == classroomId) {
                "Atividade vinculada a plano de outra turma ou inexistente no backup."
            }
            LessonActivityV7.validated(LessonActivityV7.Input(
                classroomId = classroomId,
                lessonId = lessonId,
                title = activity.getString("title"),
                instructions = activity.getString("instructions"),
                durationMinutes = activity.getInt("durationMinutes"),
            ))
        }

        // Older backups legitimately do not know an appointment's duration (end == start).''')

restore = 'TeacherBackupRestore.kt'
change(restore, 'val attendance: Int, val observations: Int, val appointments: Int, val files: Int)',
       'val attendance: Int, val observations: Int, val appointments: Int, val files: Int, val activities: Int = 0)')
change(restore, '        val appointments: List<JSONObject>, val folders: List<JSONObject>, val files: List<JSONObject>,',
       '        val appointments: List<JSONObject>, val folders: List<JSONObject>, val files: List<JSONObject>,\n        val activities: List<JSONObject>,')
change(restore, '            observations.size, appointments.size, files.size)',
       '            observations.size, appointments.size, files.size, activities.size)')
change(restore, '            files = root.getJSONArray("files").rows(),',
       '            files = root.getJSONArray("files").rows(),\n            activities = root.getJSONArray("activities").rows(),')
change(restore, '        result.lessons.uniqueIds("Planos")',
       '        result.lessons.uniqueIds("Planos")\n        result.activities.uniqueIds("Atividades")')
change(restore, '        require(db.version == LessonPlanV6.VERSION) { "Atualize o banco antes de restaurar." }',
       '        require(db.version == LessonActivityV7.VERSION) { "Atualize o banco antes de restaurar." }')
change(restore, '            listOf("attendance", "observations", "lessons", "students", "appointments", "saved_files",',
       '            listOf("lesson_activities", "attendance", "observations", "lessons", "students", "appointments", "saved_files",')
change(restore, '''            records.attendance.forEach { db.insertOrThrow("attendance", null, values(''',
'''            // Insert after classrooms and lessons: SQLite FKs and explicit class ownership are validated.
            records.activities.forEach { db.insertOrThrow("lesson_activities", null, values(
                "id" to it.id(), "classroom_id" to it.id("classroomId"),
                "lesson_id" to it.optionalId("lessonId"), "title" to it.required("title"),
                "instructions" to it.required("instructions"), "duration_minutes" to it.getInt("durationMinutes"))) }
            records.attendance.forEach { db.insertOrThrow("attendance", null, values(''')

for name, path in paths.items():
    path.write_text(contents[name], encoding='utf-8')
print('Applied full v7 activities backup: codec, export, preflight validation, transaction restore')
