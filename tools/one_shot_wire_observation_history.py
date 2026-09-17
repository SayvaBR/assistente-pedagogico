#!/usr/bin/env python3
"""Guarded one-shot patch; remove after applying so PR contains only product code."""
from pathlib import Path

ui = Path("app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt")
s = ui.read_text(encoding="utf-8")

def replace_once(old: str, new: str) -> None:
    global s
    assert s.count(old) == 1, f"Expected one anchor, found {s.count(old)}: {old[:90]}"
    s = s.replace(old, new, 1)

replace_once(
    '    ActionTile("📝", "Registros", "Nova observação pedagógica") { go("observation") }',
    '    ActionTile("📝", "Registros", "Nova observação pedagógica") { go("observation") }\n'
    '    ActionTile("▦", "Histórico de registros", "Consultar e editar todas as observações") { go("observationHistory") }',
)
replace_once(
    '                    "observation" -> if (currentClass != null) ObservationForm(',
    '                    "observationHistory" -> if (currentClass != null) ObservationHistoryScreen(\n'
    '                        snapshot, currentClass, onBack = { back() },\n'
    '                        onNew = { navigate("observation") },\n'
    '                        onOpen = { selectedObservation = it; navigate("editObservation") })\n'
    '                    "observation" -> if (currentClass != null) ObservationForm(',
)
replace_once(
    'commit("classDetail") { store.addObservation(currentClass.id, studentId, kind, body, share) }',
    'commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") { store.addObservation(currentClass.id, studentId, kind, body, share) }',
)
replace_once(
    'commit("classDetail") { store.updateObservation(currentClass.id, observation.id, studentId, kind, body, share) }',
    'commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") { store.updateObservation(currentClass.id, observation.id, studentId, kind, body, share) }',
)
replace_once(
    'delete = { commit("classDetail") { store.deleteObservation(currentClass.id, observation.id) } }',
    'delete = { commit(if (backStack.lastOrNull() == "observationHistory") "observationHistory" else "classDetail") { store.deleteObservation(currentClass.id, observation.id) } }',
)
ui.write_text(s, encoding="utf-8")

history = Path("app/src/main/java/com/sayvabr/assistentepedagogico/ui/ObservationHistoryScreen.kt")
h = history.read_text(encoding="utf-8")
assert h.count('names[note.studentId]?.') == 2
h = h.replace('names[note.studentId]?.', 'note.studentId?.let { names[it] }?.')
history.write_text(h, encoding="utf-8")
print("History route and nullable student lookup updated with guard checks.")
