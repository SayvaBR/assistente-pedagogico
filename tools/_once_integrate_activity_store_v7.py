#!/usr/bin/env python3
"""One-shot, fail-closed integration. Removed along with its workflow in the resulting commit."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/data/TeacherStore.kt'
s = p.read_text(encoding='utf-8')

def replace_once(old: str, new: str) -> None:
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'ABORT: expected one exact integration anchor, found {count}: {old!r}')
    s = s.replace(old, new, 1)

replace_once('"pedagogico.db", null, LessonPlanV6.VERSION)', '"pedagogico.db", null, LessonActivityV7.VERSION)')
replace_once('        LessonPlanV6.migrate(db)\n    }', '        LessonPlanV6.migrate(db)\n        LessonActivityV7.migrate(db)\n    }')
replace_once('oldVersion in 1..5 && newVersion == LessonPlanV6.VERSION', 'oldVersion in 1..6 && newVersion == LessonActivityV7.VERSION')
replace_once('        if (oldVersion < 6) LessonPlanV6.migrate(db)\n    }', '        if (oldVersion < 6) LessonPlanV6.migrate(db)\n        if (oldVersion < 7) LessonActivityV7.migrate(db)\n    }')
replace_once('    /** This is the only source of trashed catalog references; normal snapshots hide the trash. */', '''    /** Planning activities share this Activity-owned SQLite helper, never a second connection. */
    fun listActivities(classroomId: Long, lessonId: Long? = null): List<LessonActivityV7.Activity> =
        LessonActivityV7.list(readableDatabase, classroomId, lessonId)
    fun saveActivity(input: LessonActivityV7.Input): Long = LessonActivityV7.create(writableDatabase, input)
    fun updateActivity(activityId: Long, input: LessonActivityV7.Input) =
        LessonActivityV7.update(writableDatabase, activityId, input)
    fun duplicateActivity(classroomId: Long, activityId: Long, targetLessonId: Long? = null): Long =
        LessonActivityV7.duplicate(writableDatabase, classroomId, activityId, targetLessonId)
    fun deleteActivity(classroomId: Long, activityId: Long) =
        LessonActivityV7.delete(writableDatabase, classroomId, activityId)

    /** This is the only source of trashed catalog references; normal snapshots hide the trash. */''')
# Assert all modifications before writing anything to disk.
tests = sorted((root / 'app/src/androidTest').rglob('*.kt'))
updates = {}
for test in tests:
    original = test.read_text(encoding='utf-8')
    changed = original.replace('assertEquals(LessonPlanV6.VERSION, ', 'assertEquals(LessonActivityV7.VERSION, ')
    if changed != original:
        updates[test] = changed
if not updates:
    raise SystemExit('ABORT: expected at least one pre-v7 version assertion to update')
p.write_text(s, encoding='utf-8')
for test, changed in updates.items():
    test.write_text(changed, encoding='utf-8')
print('Integrated TeacherStore migration v6->v7 and CRUD; updated', len(updates), 'legacy version assertions')
