package com.sayvabr.assistentepedagogico.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject

data class AttendanceSessionMember(val studentId: Long, val studentName: String, val status: String)
data class AttendanceSession(
    val id: Long,
    val classroomId: Long,
    val date: String,
    val rosterComplete: Boolean,
    val members: List<AttendanceSessionMember>,
)

/** Adds immutable participant identity to each saved call without inventing old roster data. */
object AttendanceV10 {
    const val VERSION = 10

    fun migrate(db: SQLiteDatabase, captureLegacyRows: Boolean) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS attendance_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
            day TEXT NOT NULL,
            roster_complete INTEGER NOT NULL CHECK(roster_complete IN (0,1)),
            UNIQUE(classroom_id,day)
        )""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS attendance_session_members (
            session_id INTEGER NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
            student_id INTEGER NOT NULL CHECK(student_id > 0),
            student_name TEXT NOT NULL CHECK(length(trim(student_name)) > 0),
            status TEXT NOT NULL CHECK(status IN ('P','F','?')),
            PRIMARY KEY(session_id,student_id)
        )""")
        if (captureLegacyRows) captureLegacyAttendance(db)
    }

    /** Old rows prove a mark was saved, but do not prove the old roster was complete. */
    fun captureLegacyAttendance(db: SQLiteDatabase) {
        db.execSQL("""INSERT OR IGNORE INTO attendance_sessions(classroom_id,day,roster_complete)
            SELECT classroom_id,day,0 FROM attendance GROUP BY classroom_id,day""")
        db.execSQL("""INSERT OR IGNORE INTO attendance_session_members(session_id,student_id,student_name,status)
            SELECT s.id,a.student_id,st.name,a.status
            FROM attendance_sessions s
            JOIN attendance a ON a.classroom_id=s.classroom_id AND a.day=s.day
            JOIN students st ON st.id=a.student_id AND st.classroom_id=s.classroom_id""")
    }

    fun read(db: SQLiteDatabase): List<AttendanceSession> {
        data class Building(
            val id: Long,
            val classroomId: Long,
            val date: String,
            val rosterComplete: Boolean,
            val members: MutableList<AttendanceSessionMember> = mutableListOf(),
        )
        val sessions = linkedMapOf<Long, Building>()
        db.rawQuery("""SELECT s.id,s.classroom_id,s.day,s.roster_complete,
            m.student_id,m.student_name,m.status
            FROM attendance_sessions s LEFT JOIN attendance_session_members m ON m.session_id=s.id
            ORDER BY s.day DESC,s.id DESC,m.student_name COLLATE NOCASE""", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val session = sessions.getOrPut(id) {
                    Building(id, cursor.getLong(1), cursor.getString(2), cursor.getInt(3) == 1)
                }
                if (!cursor.isNull(4)) session.members += AttendanceSessionMember(
                    cursor.getLong(4), cursor.getString(5), cursor.getString(6),
                )
            }
        }
        return sessions.values.map { AttendanceSession(it.id, it.classroomId, it.date, it.rosterComplete, it.members) }
    }

    /**
     * A new call snapshots today's roster. Reopening a saved date edits only its saved members,
     * including names of deleted students; later enrollment cannot silently change the record.
     */
    fun save(db: SQLiteDatabase, classroomId: Long, day: String, marks: Map<Long, String>) {
        db.beginTransaction()
        try {
        val existing = db.rawQuery(
            "SELECT id,roster_complete FROM attendance_sessions WHERE classroom_id=? AND day=?",
            arrayOf(classroomId.toString(), day),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) to (cursor.getInt(1) == 1) else null }
        val roster = if (existing == null) {
            db.rawQuery("SELECT id,name FROM students WHERE classroom_id=? ORDER BY name COLLATE NOCASE",
                arrayOf(classroomId.toString())).use { cursor ->
                buildList { while (cursor.moveToNext()) add(AttendanceSessionMember(cursor.getLong(0), cursor.getString(1), "?")) }
            }
        } else {
            db.rawQuery("SELECT student_id,student_name,status FROM attendance_session_members WHERE session_id=? ORDER BY student_name COLLATE NOCASE",
                arrayOf(existing.first.toString())).use { cursor ->
                buildList { while (cursor.moveToNext()) add(AttendanceSessionMember(cursor.getLong(0), cursor.getString(1), cursor.getString(2))) }
            }
        }
        require(roster.isNotEmpty()) { "Cadastre alunos antes de salvar a chamada." }
        val memberIds = roster.mapTo(mutableSetOf()) { it.studentId }
        require(marks.keys.all { it in memberIds }) { "Aluno não pertence à lista desta chamada." }
        require(marks.values.all { it in setOf("P", "F", "?") }) { "Situação de frequência inválida." }

            val sessionId = existing?.first ?: db.insertOrThrow("attendance_sessions", null,
                values("classroom_id" to classroomId, "day" to day, "roster_complete" to true))
            if (existing != null) {
                db.delete("attendance_session_members", "session_id=?", arrayOf(sessionId.toString()))
            }
            val finalRoster = roster.map { member -> member.copy(status = marks[member.studentId] ?: member.status) }
            db.delete("attendance", "classroom_id=? AND day=?", arrayOf(classroomId.toString(), day))
            finalRoster.forEach { member ->
                db.insertOrThrow("attendance_session_members", null, values(
                    "session_id" to sessionId, "student_id" to member.studentId,
                    "student_name" to member.studentName, "status" to member.status,
                ))
                if (member.status == "P" || member.status == "F") {
                    // The legacy table is retained for existing summaries. Deleted students live
                    // only in the immutable session snapshot because its old FK must remain valid.
                    db.rawQuery("SELECT 1 FROM students WHERE id=? AND classroom_id=?",
                        arrayOf(member.studentId.toString(), classroomId.toString())).use { cursor ->
                        if (cursor.moveToFirst()) db.insertOrThrow("attendance", null, values(
                            "classroom_id" to classroomId, "student_id" to member.studentId,
                            "day" to day, "status" to member.status,
                        ))
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun restoreBackup(db: SQLiteDatabase, sessions: List<JSONObject>) {
        sessions.forEach { session ->
            val id = session.getLong("id")
            db.insertOrThrow("attendance_sessions", null, values(
                "id" to id, "classroom_id" to session.getLong("classroomId"),
                "day" to session.getString("date"), "roster_complete" to session.getBoolean("rosterComplete"),
            ))
            val members = session.getJSONArray("members")
            for (index in 0 until members.length()) {
                val member = members.getJSONObject(index)
                db.insertOrThrow("attendance_session_members", null, values(
                    "session_id" to id, "student_id" to member.getLong("studentId"),
                    "student_name" to member.getString("studentName"), "status" to member.getString("status"),
                ))
            }
        }
    }

    /** Historical snapshots may reference students already deleted from the live roster. */
    fun advanceStudentIdSequencePastSnapshots(db: SQLiteDatabase) {
        val historicalMax = db.rawQuery(
            "SELECT COALESCE(MAX(student_id), 0) FROM attendance_session_members", null,
        ).use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
        val liveMax = db.rawQuery("SELECT COALESCE(MAX(id), 0) FROM students", null)
            .use { cursor -> cursor.moveToFirst(); cursor.getLong(0) }
        val idHighWater = maxOf(historicalMax, liveMax)
        if (idHighWater <= 0) return

        val sequenceRow = db.rawQuery(
            "SELECT seq FROM sqlite_sequence WHERE name='students'", null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        val nextSequence = maxOf(sequenceRow ?: 0L, idHighWater)
        if (sequenceRow != null) {
            db.execSQL("UPDATE sqlite_sequence SET seq=? WHERE name='students'", arrayOf(nextSequence))
        } else {
            db.insertOrThrow("sqlite_sequence", null, values("name" to "students", "seq" to nextSequence))
        }
    }

    private fun values(vararg pairs: Pair<String, Any?>) = ContentValues().apply {
        pairs.forEach { (key, value) -> when (value) {
            null -> putNull(key)
            is Long -> put(key, value)
            is Int -> put(key, value)
            is Boolean -> put(key, if (value) 1 else 0)
            else -> put(key, value.toString())
        } }
    }
}
