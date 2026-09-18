"""One-shot, exact-match patch of existing Compose routes. Removes itself and its workflow on success."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
editor = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/LessonEditorV6.kt'
app = root / 'app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt'
workflow = root / '.github/workflows/one-shot-lesson-duplicate.yml'


def replace_once(content: str, old: str, new: str, label: str) -> str:
    if content.count(old) != 1:
        raise RuntimeError(f'{label}: expected exactly one original anchor, got {content.count(old)}')
    return content.replace(old, new, 1)


source_editor = editor.read_text(encoding='utf-8')
source_app = app.read_text(encoding='utf-8')
assert 'Duplicar plano e atividades' not in source_editor, 'Editor already patched'
assert 'store.duplicateLesson(' not in source_app, 'App already patched'

patched_editor = replace_once(
    source_editor,
    '    archive: (() -> Unit)? = null,\n    onDirty: () -> Unit = {},',
    '    archive: (() -> Unit)? = null,\n    duplicate: (() -> Unit)? = null,\n    onDirty: () -> Unit = {},',
    'editor parameter',
)
patched_editor = replace_once(
    patched_editor,
    '    var confirmArchive by remember { mutableStateOf(false) }',
    '    var confirmArchive by remember { mutableStateOf(false) }\n    var confirmDuplicate by remember { mutableStateOf(false) }',
    'editor confirmation state',
)
patched_editor = replace_once(
    patched_editor,
    '            if (archive != null) {\n                Spacer(Modifier.height(10.dp))\n                ApRaisedButton("Arquivar plano", onClick = { confirmArchive = true }, glyph = ApGlyphKind.FOLDER, secondary = true)\n            }',
    '            if (duplicate != null) {\n                Spacer(Modifier.height(10.dp))\n                ApRaisedButton("Duplicar plano e atividades", onClick = { confirmDuplicate = true }, glyph = ApGlyphKind.PLUS, secondary = true)\n            }\n            if (archive != null) {\n                Spacer(Modifier.height(10.dp))\n                ApRaisedButton("Arquivar plano", onClick = { confirmArchive = true }, glyph = ApGlyphKind.FOLDER, secondary = true)\n            }',
    'duplicate button',
)
patched_editor = replace_once(
    patched_editor,
    '    if (confirmArchive && archive != null) AlertDialog(\n        onDismissRequest = { confirmArchive = false },\n        title = { Text("Arquivar plano?") },\n        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados.") },\n        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },\n        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },\n    )',
    '    if (confirmArchive && archive != null) AlertDialog(\n        onDismissRequest = { confirmArchive = false },\n        title = { Text("Arquivar plano?") },\n        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados.") },\n        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },\n        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },\n    )\n    if (confirmDuplicate && duplicate != null) AlertDialog(\n        onDismissRequest = { confirmDuplicate = false },\n        title = { Text("Duplicar plano e atividades?") },\n        text = { Text("Uma cópia independente do plano e de suas atividades vinculadas será criada na mesma turma. Alterações ainda não salvas nesta tela não serão copiadas.") },\n        confirmButton = { TextButton(onClick = { confirmDuplicate = false; duplicate() }) { Text("Duplicar") } },\n        dismissButton = { TextButton(onClick = { confirmDuplicate = false }) { Text("Cancelar") } },\n    )',
    'duplicate confirmation dialog',
)
patched_app = replace_once(
    source_app,
    '                                archive = { commit("planning") { store.setLessonArchived(lessonClass.id, lesson.id, true) } },\n                                onDirty = { formDirty = true })',
    '                                archive = { commit("planning") { store.setLessonArchived(lessonClass.id, lesson.id, true) } },\n                                duplicate = { commit("planning", onSuccess = { selectedDay = lesson.date }) {\n                                    store.duplicateLesson(lessonClass.id, lesson.id)\n                                } },\n                                onDirty = { formDirty = true })',
    'edit plan route',
)

# All anchors are validated before any write; do not leave half-applied UI changes.
editor.write_text(patched_editor, encoding='utf-8')
app.write_text(patched_app, encoding='utf-8')
workflow.unlink()
Path(__file__).unlink()
print('Patched editor and route; temporary workflow/script deleted.')
