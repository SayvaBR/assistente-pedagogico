package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.net.Uri

/**
 * Imports only a SAF reference. The caller must acquire the persistent read permission first.
 * Re-selecting the same document must not silently do nothing when it is in the trash or
 * inaccessible. This operation never reads, copies, renames or deletes the source document.
 */
object FileImportPolicy {
    fun importReference(db: SQLiteDatabase, name: String, uri: String): Long {
        val label = name.trim()
        val parsed = Uri.parse(uri)
        require(label.isNotEmpty() && label.length <= 180) { "Informe um nome de arquivo válido (até 180 caracteres)." }
        require(parsed.scheme == "content" && !parsed.authority.isNullOrBlank()) { "Selecione um documento válido no Android." }

        db.beginTransaction()
        try {
            val existingId = db.rawQuery("SELECT id FROM saved_files WHERE uri=?", arrayOf(uri)).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
            val id = if (existingId == null) {
                db.insertOrThrow("saved_files", null, ContentValues().apply {
                    put("name", label)
                    put("uri", uri)
                    put("access_state", "available")
                })
            } else {
                // Preserve the user's catalog name, chosen folder and favorite flag. Only restore
                // visibility and acknowledge the newly granted SAF access. Keep the stable ID.
                val updated = db.update("saved_files", ContentValues().apply {
                    putNull("trashed_at")
                    put("access_state", "available")
                }, "id=?", arrayOf(existingId.toString()))
                check(updated == 1) { "O arquivo foi alterado; tente importar novamente." }
                existingId
            }
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }
}
