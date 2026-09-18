package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

/** Schema v9 preserves every v8 lesson and creates layouts lazily for existing plans. */
object PlanLayoutV9 {
    const val VERSION = 9
    private const val MAX_BYTES = 512 * 1024

    fun migrate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS lesson_layouts (
            lesson_id INTEGER PRIMARY KEY REFERENCES lessons(id) ON DELETE CASCADE,
            layout_json TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS plan_templates (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL COLLATE NOCASE UNIQUE,
            layout_json TEXT NOT NULL
        )""")
    }

    /** A native block only controls the presentation of a canonical field; never duplicate its content. */
    fun encode(composition: PlanComposition): String = JSONObject().apply {
        put("version", 1)
        put("blocks", JSONArray().apply {
            composition.blocks.forEach { block ->
                require(block.kind == PlanBlockKind.CUSTOM || (block.body.isEmpty() && block.minutes == null)) {
                    "Campos nativos devem permanecer vinculados ao plano, sem cópias no layout."
                }
                put(JSONObject().apply {
                    put("id", block.id)
                    put("kind", block.kind.name)
                    put("title", block.title)
                    put("body", block.body)
                    if (block.minutes != null) put("minutes", block.minutes)
                })
            }
        })
    }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Estrutura do plano excede o limite de segurança." } }

    fun decode(raw: String): PlanComposition {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Estrutura do plano muito extensa." }
        val root = JSONObject(raw)
        require(root.getInt("version") == 1) { "Versão da estrutura do plano não suportada." }
        val array = root.getJSONArray("blocks")
        require(array.length() in 1..40) { "Quantidade de seções inválida." }
        val blocks = (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            val kind = PlanBlockKind.valueOf(row.getString("kind"))
            val block = PlanBlock(
                id = row.getString("id"), kind = kind, title = row.getString("title"),
                body = row.optString("body", ""),
                minutes = if (row.has("minutes") && !row.isNull("minutes")) row.getInt("minutes") else null,
            )
            require(kind == PlanBlockKind.CUSTOM || (block.body.isEmpty() && block.minutes == null)) {
                "Conteúdo nativo duplicado na estrutura do plano."
            }
            block
        }
        return PlanComposition(blocks)
    }

    fun get(db: SQLiteDatabase, classroomId: Long, lessonId: Long): PlanComposition {
        require(classroomId > 0 && lessonId > 0) { "Turma ou plano inválido." }
        db.rawQuery("SELECT 1 FROM lessons WHERE id=? AND classroom_id=?", arrayOf(lessonId.toString(), classroomId.toString())).use {
            require(it.moveToFirst()) { "Plano não pertence a esta turma." }
        }
        return db.rawQuery("SELECT layout_json FROM lesson_layouts WHERE lesson_id=?", arrayOf(lessonId.toString())).use {
            if (it.moveToFirst()) decode(it.getString(0)) else PlanComposition.standard()
        }
    }

    /** Caller owns the surrounding transaction: lesson and layout succeed or fail together. */
    fun put(db: SQLiteDatabase, classroomId: Long, lessonId: Long, layout: PlanComposition) {
        require(lessonId > 0 && classroomId > 0)
        val payload = encode(layout)
        db.rawQuery("SELECT 1 FROM lessons WHERE id=? AND classroom_id=? AND archived=0 AND pedagogical_status='draft'",
            arrayOf(lessonId.toString(), classroomId.toString())).use {
            require(it.moveToFirst()) { "Somente rascunhos da turma podem ter a estrutura alterada." }
        }
        val values = ContentValues().apply { put("lesson_id", lessonId); put("layout_json", payload) }
        require(db.insertWithOnConflict("lesson_layouts", null, values, SQLiteDatabase.CONFLICT_REPLACE) > 0L)
    }

    fun listTemplates(db: SQLiteDatabase): List<Pair<Long, PlanTemplate>> = buildList {
        db.rawQuery("SELECT id,name,layout_json FROM plan_templates ORDER BY name COLLATE NOCASE", null).use { cursor ->
            while (cursor.moveToNext()) add(cursor.getLong(0) to PlanTemplate(cursor.getString(1), decode(cursor.getString(2)).blocks))
        }
    }

    fun createTemplate(db: SQLiteDatabase, template: PlanTemplate): Long {
        val payload = encode(template.instantiate())
        val values = ContentValues().apply { put("name", template.name.trim()); put("layout_json", payload) }
        return db.insertOrThrow("plan_templates", null, values).also { require(it > 0L) }
    }

    fun deleteTemplate(db: SQLiteDatabase, id: Long) {
        require(db.delete("plan_templates", "id=?", arrayOf(id.toString())) == 1) { "Modelo não encontrado." }
    }

    fun allLayouts(db: SQLiteDatabase): List<Pair<Long, String>> = buildList {
        db.rawQuery("SELECT lesson_id,layout_json FROM lesson_layouts ORDER BY lesson_id", null).use { cursor ->
            while (cursor.moveToNext()) add(cursor.getLong(0) to encode(decode(cursor.getString(1))))
        }
    }
}

/** All composed writes use the same Activity-owned helper, with a single atomic transaction. */
fun TeacherStore.createComposedLesson(classroomId: Long, input: LessonPlanV6.Input, layout: PlanComposition): Long {
    val db = writableDatabase
    db.beginTransaction()
    return try {
        val id = saveLesson(input, classroomId)
        PlanLayoutV9.put(db, classroomId, id, layout)
        db.setTransactionSuccessful()
        id
    } finally { db.endTransaction() }
}

fun TeacherStore.updateComposedLesson(classroomId: Long, lessonId: Long, input: LessonPlanV6.Input, layout: PlanComposition) {
    require(input.statusTransition == null) { "Mudanças de estado exigem confirmação separada." }
    val db = writableDatabase
    db.beginTransaction()
    try {
        updateLesson(classroomId, lessonId, input)
        PlanLayoutV9.put(db, classroomId, lessonId, layout)
        db.setTransactionSuccessful()
    } finally { db.endTransaction() }
}

fun TeacherStore.lessonComposition(classroomId: Long, lessonId: Long): PlanComposition =
    PlanLayoutV9.get(readableDatabase, classroomId, lessonId)

fun TeacherStore.planTemplates(): List<Pair<Long, PlanTemplate>> = PlanLayoutV9.listTemplates(readableDatabase)
fun TeacherStore.createPlanTemplate(template: PlanTemplate): Long = PlanLayoutV9.createTemplate(writableDatabase, template)
fun TeacherStore.deletePlanTemplate(id: Long) = PlanLayoutV9.deleteTemplate(writableDatabase, id)
