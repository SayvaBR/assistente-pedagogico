#!/usr/bin/env python3
"""One-time scoped source integration, run on pinned branch SHA; self-deleted by workflow.
No database migration or user records touched. Abort if source changed unexpectedly.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui'
app_path = ui / 'TeacherApp.kt'
workspace_path = ui / 'PlanningWorkspace.kt'
source = app_path.read_text(encoding='utf-8')
workspace = workspace_path.read_text(encoding='utf-8')


def replace_once(original: str, replacement: str) -> None:
    global source
    count = source.count(original)
    if count != 1:
        raise SystemExit(f'Refusing to patch TeacherApp: expected 1 occurrence, found {count}: {original[:90]!r}')
    source = source.replace(original, replacement, 1)


# Keep the routed API stable while replacing old list-only calendar with real screens.
start = '@Composable private fun PlanningScreen(data: TeacherSnapshot, classroom: Classroom?, day: String, onDay: (String) -> Unit,'
end = '@Composable private fun LessonForm(classroom: Classroom, initialDay: String, back: () -> Unit,'
if source.count(start) != 1 or source.count(end) != 1:
    raise SystemExit('PlanningScreen boundaries changed; no changes made')
begin = source.index(start)
finish = source.index(end)
if finish <= begin:
    raise SystemExit('Invalid PlanningScreen boundaries')
source = source[:begin] + '''@Composable private fun PlanningScreen(data: TeacherSnapshot, classroom: Classroom?, day: String, onDay: (String) -> Unit,
    go: (String) -> Unit, openLesson: (Long) -> Unit, restoreLesson: (Lesson) -> Unit) {
    PlanningWorkspace(data, classroom, day, onDay, go, openLesson, restoreLesson)
}

''' + source[finish:]

agenda_start = '@Composable private fun AgendaScreen(data: TeacherSnapshot, day: String, onDay: (String) -> Unit, add: () -> Unit, open: (Long) -> Unit) {'
agenda_end = '@Composable private fun AppointmentForm(initialDay: String, back: () -> Unit, save: (String, String, String) -> Unit,'
if source.count(agenda_start) != 1 or source.count(agenda_end) != 1:
    raise SystemExit('AgendaScreen boundaries changed; no changes made')
begin = source.index(agenda_start)
finish = source.index(agenda_end)
if finish <= begin:
    raise SystemExit('Invalid AgendaScreen boundaries')
source = source[:begin] + '''@Composable private fun AgendaScreen(data: TeacherSnapshot, day: String, onDay: (String) -> Unit,
    add: () -> Unit, open: (Long) -> Unit, openLesson: (Long) -> Unit) {
    PlanningAgendaScreen(data, day, onDay, add, open, openLesson)
}

''' + source[finish:]

replace_once(
    '''"agenda" -> AgendaScreen(snapshot, selectedDay, { selectedDay = it }, { requestNavigate("newAppointment") }, { appointmentId ->
                        selectedAppointment = appointmentId; requestNavigate("editAppointment")
                    })''',
    '''"agenda" -> AgendaScreen(snapshot, selectedDay, { selectedDay = it }, { requestNavigate("newAppointment") }, { appointmentId ->
                        selectedAppointment = appointmentId; requestNavigate("editAppointment")
                    }, { lessonId -> selectedLesson = lessonId; requestNavigate("editLesson") })''',
)

bad = 'value.replaceFirstChar { it.uppercase(Locale("pt", "BR")) }'
good = 'value.replaceFirstChar { it.uppercase() }'
if workspace.count(bad) != 1:
    raise SystemExit('Expected capitalization compatibility fix exactly once')
workspace = workspace.replace(bad, good, 1)

# Guard against empty placeholders and accidental removal of existing editors/routes.
for anchor in ('"newLesson" ->', '"editLesson" ->', '"newAppointment" ->', '"editAppointment" ->', '"planning" ->', '"agenda" ->'):
    if source.count(anchor) < 1:
        raise SystemExit(f'Missing existing route after integration: {anchor}')
if source.count('PlanningWorkspace(data, classroom, day, onDay, go, openLesson, restoreLesson)') != 1:
    raise SystemExit('Planning must be wired once and only once')
if source.count('PlanningAgendaScreen(data, day, onDay, add, open, openLesson)') != 1:
    raise SystemExit('Agenda must be wired once and only once')

app_path.write_text(source, encoding='utf-8')
workspace_path.write_text(workspace, encoding='utf-8')
print('Integrated real Planning/Agenda calendar with existing CRUD navigation; corrected Kotlin capitalization.')
