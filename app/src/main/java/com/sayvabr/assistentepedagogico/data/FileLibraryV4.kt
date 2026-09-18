package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.time.Instant

/** A folder is a catalog grouping. It never owns or deletes the original SAF documents. */
data class FileFolder(val id: Long, val name: String)

data class LibraryFile(
    val id: Long,
    val name: String,
    val uri: String,
    val folderId: Long?,
    val favorite: Boolean,
    val trashedAt: String?,
    val accessState: String,
)

/**
 * Additive schema and actual operations for the existing private pedagogico.db. The upgrade
 * itself is transactional in SQLiteOpenHelper. Re-entry is harmless for a partially prepared
 * schema/legacy test fixture; never drop a user table or copy document content.
 */
object FileLibraryV4 {
    const val VERSION = 4

    fun migrate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS file_folders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                "created_at TEXT NOT NULL)"
        )
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(saved_files)", null).use { cursor ->
            while (cursor.moveToNext()) columns += cursor.getString(1)
        }
        require(columns.containsAll(setOf("id", "name", "uri"))) { "O catálogo existente está incompleto; migração cancelada." }
        // Nullable REFERENCES defaults to NULL, preserving legacy rows and original IDs/URIs.
        if ("folder_id" !in columns) db.execSQL("ALTER TABLE saved_files ADD COLUMN folder_id INTEGER REFERENCES file_folders(id) ON DELETE SET NULL")
        if ("favorite" !in columns) db.execSQL("ALTER TABLE saved_files ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1))")
        if ("trashed_at" !in columns) db.execSQL("ALTER TABLE saved_files ADD COLUMN trashed_at TEXT DEFAULT NULL")
        if ("access_state" !in columns) db.execSQL("ALTER TABLE saved_files ADD COLUMN access_state TEXT NOT NULL DEFAULT 'unknown' CHECK(access_state IN ('unknown','available','revoked'))")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_files_folder ON saved_files(folder_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_saved_files_trash ON saved_files(trashed_at)")
    }

    fun folders(db: SQLiteDatabase): List<FileFolder> = buildList {
        db.rawQuery("SELECT id,name FROM file_folders ORDER BY name COLLATE NOCASE", null).use { cursor ->
            while (cursor.moveToNext()) add(FileFolder(cursor.getLong(0), cursor.getString(1)))
        }
    }

    /** Include trash intentionally; the UI chooses a view and ordinary snapshots hide trash. */
    fun files(db: SQLiteDatabase): List<LibraryFile> = buildList {
        db.rawQuery("SELECT id,name,uri,folder_id,favorite,trashed_at,access_state FROM saved_files ORDER BY id DESC", null).use { cursor ->
            while (cursor.moveToNext()) add(
                LibraryFile(
                    id = cursor.getLong(0), name = cursor.getString(1), uri = cursor.getString(2),
                    folderId = if (cursor.isNull(3)) null else cursor.getLong(3),
                    favorite = cursor.getInt(4) == 1,
                    trashedAt = if (cursor.isNull(5)) null else cursor.getString(5),
                    accessState = cursor.getString(6),
                )
            )
        }
    }

    fun createFolder(db: SQLiteDatabase, name: String): Long = db.insertOrThrow(
        "file_folders", null, ContentValues().apply {
            put("name", validatedName(name))
            put("created_at", Instant.now().toString())
        },
    )

    fun renameFolder(db: SQLiteDatabase, folderId: Long, name: String) {
        val changed = db.update("file_folders", ContentValues().apply { put("name", validatedName(name)) },
            "id=?", arrayOf(folderId.toString()))
        require(changed == 1) { "Pasta não encontrada." }
    }

    /** No original document or file reference may be removed by deleting a folder. */
    fun deleteFolder(db: SQLiteDatabase, folderId: Long) {
        db.beginTransaction()
        try {
            require(exists(db, "file_folders", folderId)) { "Pasta não encontrada." }
            db.execSQL("UPDATE saved_files SET folder_id=NULL WHERE folder_id=?", arrayOf(folderId))
            require(db.delete("file_folders", "id=?", arrayOf(folderId.toString())) == 1)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun moveFile(db: SQLiteDatabase, fileId: Long, folderId: Long?) {
        if (folderId != null) require(exists(db, "file_folders", folderId)) { "Pasta de destino não encontrada." }
        val changed = db.update("saved_files", ContentValues().apply {
            if (folderId == null) putNull("folder_id") else put("folder_id", folderId)
        }, "id=? AND trashed_at IS NULL", arrayOf(fileId.toString()))
        require(changed == 1) { "Arquivo não encontrado ou está na lixeira." }
    }

    fun setFavorite(db: SQLiteDatabase, fileId: Long, favorite: Boolean) {
        val changed = db.update("saved_files", ContentValues().apply { put("favorite", if (favorite) 1 else 0) },
            "id=? AND trashed_at IS NULL", arrayOf(fileId.toString()))
        require(changed == 1) { "Arquivo não encontrado ou está na lixeira." }
    }

    fun trash(db: SQLiteDatabase, fileId: Long) {
        val changed = db.update("saved_files", ContentValues().apply { put("trashed_at", Instant.now().toString()) },
            "id=? AND trashed_at IS NULL", arrayOf(fileId.toString()))
        require(changed == 1) { "Arquivo não encontrado ou já está na lixeira." }
    }

    fun restore(db: SQLiteDatabase, fileId: Long) {
        val changed = db.update("saved_files", ContentValues().apply { putNull("trashed_at") },
            "id=? AND trashed_at IS NOT NULL", arrayOf(fileId.toString()))
        require(changed == 1) { "Arquivo não encontrado na lixeira." }
    }

    /** Revoked SAF permissions leave metadata, folder, favorite and trash untouched. */
    fun setAccessState(db: SQLiteDatabase, fileId: Long, state: String) {
        require(state in setOf("unknown", "available", "revoked")) { "Estado de acesso inválido." }
        val changed = db.update("saved_files", ContentValues().apply { put("access_state", state) },
            "id=?", arrayOf(fileId.toString()))
        require(changed == 1) { "Arquivo não encontrado." }
    }

    /** Permanent deletion affects an already-trashed catalog reference only, never the source URI. */
    fun permanentlyRemoveReference(db: SQLiteDatabase, fileId: Long) {
        val deleted = db.delete("saved_files", "id=? AND trashed_at IS NOT NULL", arrayOf(fileId.toString()))
        require(deleted == 1) { "Envie o vínculo à lixeira antes de excluí-lo definitivamente." }
    }

    private fun validatedName(name: String): String = name.trim().also {
        require(it.isNotEmpty() && it.length <= 80 && !it.contains('/')) {
            "Informe um nome de pasta válido (até 80 caracteres, sem barra)."
        }
    }

    private fun exists(db: SQLiteDatabase, table: String, id: Long): Boolean {
        require(id > 0) { "Identificador inválido." }
        db.rawQuery("SELECT 1 FROM $table WHERE id=?", arrayOf(id.toString())).use { return it.moveToFirst() }
    }
}
