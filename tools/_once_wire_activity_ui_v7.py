#!/usr/bin/env python3
"""Guarded, one-shot activity UI wiring. Workflow removes this file after applying."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt'
planning = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/PlanningWorkspace.kt'
a = app.read_text(encoding='utf-8')
p = planning.read_text(encoding='utf-8')
old_route = '                    "newLesson" -> if (currentClass != null) LessonEditorV6('
new_route = '''                    "activities" -> PlanningActivitiesScreen(
                        store = store,
                        classroom = currentClass,
                        lessons = snapshot.lessons,
                        back = { requestBack() },
                        onDirty = { formDirty = it },
                    )
''' + old_route
if a.count(old_route) != 1 or '"activities" -> PlanningActivitiesScreen(' in a:
    raise SystemExit('ABORT: navigation anchor changed or activities screen already wired')
a = a.replace(old_route, new_route, 1)
old_button = '        ApRaisedButton("Ver compromissos", onClick = { go("agenda") }, glyph = ApGlyphKind.CALENDAR, secondary = true)'
new_button = old_button + '''
        Spacer(Modifier.height(9.dp))
        ApRaisedButton("Atividades da turma", onClick = { go("activities") }, glyph = ApGlyphKind.DOCUMENT, secondary = true)'''
if p.count(old_button) != 1 or 'Atividades da turma' in p:
    raise SystemExit('ABORT: planning action anchor changed or activities action already wired')
p = p.replace(old_button, new_button, 1)
# Both files are validated before either file is changed.
app.write_text(a, encoding='utf-8')
planning.write_text(p, encoding='utf-8')
print('Wired activity screen and Planning CTA without changing unrelated routes')
