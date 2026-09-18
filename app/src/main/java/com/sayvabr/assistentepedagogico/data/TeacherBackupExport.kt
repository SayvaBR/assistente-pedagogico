package com.sayvabr.assistentepedagogico.data

/**
 * Export the complete local catalog, including references in the reversible trash.
 * TeacherStore.read() intentionally hides trashed files from normal screens, so exporting
 * that snapshot directly would silently discard them on a subsequent restore.
 *
 * Only catalog metadata and content:// references are exported. Document bytes and Android
 * SAF grants cannot be exported; restore must mark every reference for reauthorization.
 */
fun TeacherStore.exportBackupPayload(): String {
    val snapshot = read()
    val fullCatalog = libraryFiles().map { file ->
        SavedFile(
            id = file.id,
            name = file.name,
            uri = file.uri,
            folderId = file.folderId,
            favorite = file.favorite,
            trashedAt = file.trashedAt,
            accessState = "revoked",
        )
    }
    val activities = snapshot.classrooms.flatMap { listActivities(it.id) }
    val archiveOrigins = mutableMapOf<Long, LessonStatus>()
    readableDatabase.rawQuery("SELECT id,status_before_archive FROM lessons WHERE pedagogical_status='archived'", null).use { cursor ->
        while (cursor.moveToNext()) {
            val previous = LessonStatus.parse(cursor.getString(1))
            require(previous != LessonStatus.ARCHIVED) { "Estado anterior de plano inválido no banco." }
            archiveOrigins[cursor.getLong(0)] = previous
        }
    }
    val layouts = PlanLayoutV9.allLayouts(readableDatabase)
    val templates = planTemplates().map { (id, template) ->
        Triple(id, template.name, PlanLayoutV9.encode(template.instantiate()))
    }
    return TeacherBackupCodec.encode(snapshot.copy(files = fullCatalog), activities, archiveOrigins, layouts, templates)
}
