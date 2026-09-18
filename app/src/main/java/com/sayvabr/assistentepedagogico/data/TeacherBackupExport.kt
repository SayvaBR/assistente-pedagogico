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
    return TeacherBackupCodec.encode(snapshot.copy(files = fullCatalog))
}
