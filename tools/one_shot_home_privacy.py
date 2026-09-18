#!/usr/bin/env python3
"""One-time exact-match integration of classroom-scoped Home; self-delete after use.

Requires an isolated owner branch, a fast-forward push and the known pre-patch file blob.
Never processes actual pedagogical records; changes source text only.
"""
from pathlib import Path
import hashlib

root = Path(__file__).resolve().parents[1]
path = root / "app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt"
source = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global source
    matches = source.count(old)
    if matches != 1:
        raise SystemExit(f"Refusing unsafe patch: expected one match, found {matches}: {old[:65]!r}")
    source = source.replace(old, new, 1)


replace_once(
    '"home" -> HomeScreen(snapshot, currentClass, { requestNavigate(it) }, { selectedClass = it })',
    '''"home" -> HomeScreen(snapshot, currentClass, { destination ->
                        if (destination in setOf("attendance", "newLesson", "planning", "agenda")) selectedDay = today()
                        requestNavigate(destination)
                    }, { selectedClass = it })''',
)
replace_once(
    '''    val activeClasses = data.classrooms.filterNot { it.archived }
    if (activeClasses.size > 1) {
        Text("Turma em foco", color = ink, fontWeight = FontWeight.Bold)
        Choices(activeClasses.map { it.name }, classroom.name) { label -> activeClasses.firstOrNull { it.name == label }?.let { select(it.id) } }
    }
    val todayLesson = data.lessons.firstOrNull { it.date == today() && it.classroomId == classroom.id }''',
    '''    val activeClasses = data.classrooms.filterNot { it.archived }
    val homeDay = today()
    val overview = HomeContentPolicy.forClass(data, classroom, homeDay)
    if (activeClasses.size > 1) {
        Text("Turma em foco", color = ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeClasses.forEach { option ->
                val chosen = option.id == classroom.id
                val sameNameAndShift = activeClasses.count { it.name == option.name && it.shift == option.shift } > 1
                val label = "${option.name} · ${option.shift}" + if (sameNameAndShift) " · turma ${option.id}" else ""
                OutlinedButton(
                    onClick = { select(option.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(ApShapeToken.Medium),
                    border = BorderStroke(1.dp, if (chosen) blue else outline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (chosen) blue else Color.White),
                ) { Text(label, color = if (chosen) Color.White else ink, fontWeight = FontWeight.ExtraBold) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    val todayLesson = overview.lesson''',
)
replace_once(
    'ActionTile(ApGlyphKind.USERS, "Fazer chamada", "${classroom.name} • ${friendlyDay(today())}") { go("attendance") }',
    'ActionTile(ApGlyphKind.USERS, "Fazer chamada", "${classroom.name} • ${overview.attendanceMarked}/${overview.studentCount} registros hoje") { go("attendance") }',
)
replace_once(
    'val events = data.appointments.filter { it.date == today() }',
    'val events = data.appointments.filter { it.date == homeDay }',
)
replace_once(
    '''    data.observations.take(3).forEach { note -> ActionTile(ApGlyphKind.DOCUMENT, note.kind, note.body.take(75)) { go("classDetail") } }
    if (data.observations.isEmpty()) Panel { Text("Os registros que você criar aparecerão aqui.", color = ink) }''',
    '''    overview.recentObservations.forEach { note ->
        ActionTile(ApGlyphKind.DOCUMENT, note.kind, note.body.take(75)) { go("observationHistory") }
    }
    if (overview.recentObservations.isEmpty()) Panel {
        Text("Nenhum registro nesta turma. Suas observações de outras turmas permanecem separadas.", color = ink)
    }''',
)
if 'data.observations.take(3)' in source:
    raise SystemExit('Unsafe Home still references global observation history')
path.write_text(source, encoding="utf-8")
print('Patched Home with classroom-scoped projection, stable class IDs and contextual today navigation')
