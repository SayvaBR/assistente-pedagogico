#!/usr/bin/env python3
"""Temporary guarded source patch. Removed in the one-shot workflow after commit."""
from pathlib import Path

path = Path("app/src/main/java/com/sayvabr/assistentepedagogico/data/TeacherStore.kt")
source = path.read_text(encoding="utf-8")
old = '''        require(kind in listOf("Comportamento", "Participação", "Aprendizagem", "Outro"))
        writableDatabase.insertOrThrow("observations", null, values("classroom_id" to classroomId, "student_id" to studentId, "kind" to kind, "body" to body.trim(), "day" to LocalDate.now().toString(), "share_approved" to shareApproved))'''
new = '''        require(kind in listOf("Comportamento", "Participação", "Aprendizagem", "Outro")) { "Tipo de observação inválido." }
        // A SQLite foreign key proves that a student exists but NOT that the student
        // belongs to this classroom. Prevent cross-class links when creating notes.
        if (studentId != null) {
            readableDatabase.rawQuery("SELECT 1 FROM students WHERE id=? AND classroom_id=?", arrayOf(studentId.toString(), classroomId.toString())).use { cursor ->
                require(cursor.moveToFirst()) { "Aluno não pertence a esta turma." }
            }
        }
        writableDatabase.insertOrThrow("observations", null, values("classroom_id" to classroomId, "student_id" to studentId, "kind" to kind, "body" to body.trim(), "day" to LocalDate.now().toString(), "share_approved" to shareApproved))'''
assert source.count(old) == 1, "Unexpected TeacherStore version: refusing to patch"
path.write_text(source.replace(old, new, 1), encoding="utf-8")
print("Validated: create-observation now checks student ownership before insertion")
