#!/usr/bin/env python3
from pathlib import Path

path = Path('app/src/main/java/com/sayvabr/assistentepedagogico/data/TeacherStore.kt')
s = path.read_text(encoding='utf-8')

def repl(a,b):
    global s
    if s.count(a) != 1:
        raise SystemExit(f'Expected exactly one occurrence, found {s.count(a)}: {a[:90]}')
    s=s.replace(a,b,1)

repl('data class Lesson(val id: Long, val classroomId: Long, val title: String, val subject: String, val date: String, val time: String, val objective: String, val content: String, val method: String, val archived: Boolean = false)', '''data class Lesson(val id: Long, val classroomId: Long, val title: String, val subject: String, val date: String, val time: String, val objective: String, val content: String, val method: String, val archived: Boolean = false,
    val durationMinutes: Int = 50, val specificObjectives: String = "", val bnccCodes: String = "", val justification: String = "",
    val opening: String = "", val openingMinutes: Int = 0, val development: String = "", val developmentMinutes: Int = 0,
    val closing: String = "", val closingMinutes: Int = 0, val assessment: String = "", val adaptations: String = "")''')
repl('SQLiteOpenHelper(context.applicationContext, "pedagogico.db", null, AppointmentV5.VERSION)', 'SQLiteOpenHelper(context.applicationContext, "pedagogico.db", null, LessonPlanV6.VERSION)')
repl('        AppointmentV5.migrate(db)\n    }', '        AppointmentV5.migrate(db)\n        LessonPlanV6.migrate(db)\n    }')
repl('require(oldVersion in 1..4 && newVersion == AppointmentV5.VERSION)', 'require(oldVersion in 1..5 && newVersion == LessonPlanV6.VERSION)')
repl('        if (oldVersion < 5) AppointmentV5.migrate(db)\n    }', '        if (oldVersion < 5) AppointmentV5.migrate(db)\n        if (oldVersion < 6) LessonPlanV6.migrate(db)\n    }')
repl('db.rawQuery("SELECT id,classroom_id,title,subject,day,time,objective,content,method,archived FROM lessons ORDER BY day,time", null).use { c -> while (c.moveToNext()) lessons += Lesson(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9) == 1) }', '''db.rawQuery("SELECT id,classroom_id,title,subject,day,time,objective,content,method,archived,duration_minutes,specific_objectives,bncc_codes,justification,opening,opening_minutes,development,development_minutes,closing,closing_minutes,assessment,adaptations FROM lessons ORDER BY day,time", null).use { c -> while (c.moveToNext()) lessons += Lesson(
            c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getInt(9) == 1,
            c.getInt(10), c.getString(11), c.getString(12), c.getString(13), c.getString(14), c.getInt(15), c.getString(16), c.getInt(17), c.getString(18), c.getInt(19), c.getString(20), c.getString(21)) }''')
anchor='''    /** Reversible archival preserves the lesson, classroom and all historical data. */
    fun setLessonArchived'''
insert='''    fun saveLesson(input: LessonPlanV6.Input, classroomId: Long): Long {
        val value = LessonPlanV6.validated(input)
        val id = writableDatabase.insertOrThrow("lessons", null, values(
            "classroom_id" to classroomId, "title" to value.title, "subject" to value.subject, "day" to value.day, "time" to value.time,
            "duration_minutes" to value.durationMinutes, "objective" to value.objective, "specific_objectives" to value.specificObjectives,
            "content" to value.content, "bncc_codes" to value.bnccCodes, "justification" to value.justification, "method" to value.method,
            "opening" to value.opening, "opening_minutes" to value.openingMinutes, "development" to value.development,
            "development_minutes" to value.developmentMinutes, "closing" to value.closing, "closing_minutes" to value.closingMinutes,
            "assessment" to value.assessment, "adaptations" to value.adaptations))
        require(id > 0) { "Não foi possível salvar o plano de aula." }
        return id
    }

    fun updateLesson(classroomId: Long, lessonId: Long, input: LessonPlanV6.Input) {
        val value = LessonPlanV6.validated(input)
        val changed = writableDatabase.update("lessons", values(
            "title" to value.title, "subject" to value.subject, "day" to value.day, "time" to value.time,
            "duration_minutes" to value.durationMinutes, "objective" to value.objective, "specific_objectives" to value.specificObjectives,
            "content" to value.content, "bncc_codes" to value.bnccCodes, "justification" to value.justification, "method" to value.method,
            "opening" to value.opening, "opening_minutes" to value.openingMinutes, "development" to value.development,
            "development_minutes" to value.developmentMinutes, "closing" to value.closing, "closing_minutes" to value.closingMinutes,
            "assessment" to value.assessment, "adaptations" to value.adaptations),
            "id=? AND classroom_id=? AND archived=0", arrayOf(lessonId.toString(), classroomId.toString()))
        require(changed == 1) { "Plano não encontrado nesta turma ou está arquivado." }
    }

'''+anchor
repl(anchor, insert)
path.write_text(s, encoding='utf-8')
