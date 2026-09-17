#!/usr/bin/env python3
"""One-shot targeted patch for homonymous students; removed by workflow."""
from pathlib import Path
path = Path('app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt')
s = path.read_text(encoding='utf-8')
old = '''    var confirmDeletion by remember { mutableStateOf(false) }
    Heading(if (initial == null) "Nova observação" else "Editar observação", if (initial == null) "Registre uma observação sobre a turma" else "Registro de ${initial.date}", back)
    Panel {
        Text("Aluno (opcional)", fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(8.dp))
        Choices(listOf("Turma inteira") + data.students.filter { it.classroomId == classroom.id }.map { it.name },
            if (studentId == -1L) "Turma inteira" else data.students.firstOrNull { it.id == studentId }?.name ?: "Turma inteira") { label ->
            studentId = data.students.firstOrNull { it.classroomId == classroom.id && it.name == label }?.id ?: -1L
        }
    }'''
new = '''    var confirmDeletion by remember { mutableStateOf(false) }
    var studentPickerExpanded by remember { mutableStateOf(false) }
    // Names are not unique. Select by stable student ID to avoid attaching a
    // sensitive pedagogical note to the wrong child when two names match.
    val classroomStudents = data.students.filter { it.classroomId == classroom.id }
    val selectedStudent = classroomStudents.firstOrNull { it.id == studentId }
    fun studentLabel(student: Student): String = if (classroomStudents.count { it.name == student.name } > 1)
        "${student.name} · cadastro ${student.id}" else student.name
    Heading(if (initial == null) "Nova observação" else "Editar observação", if (initial == null) "Registre uma observação sobre a turma" else "Registro de ${initial.date}", back)
    Panel {
        Text("Aluno (opcional)", fontWeight = FontWeight.Bold, color = ink)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { studentPickerExpanded = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)) {
                Text(selectedStudent?.let(::studentLabel) ?: "Turma inteira", maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = studentPickerExpanded, onDismissRequest = { studentPickerExpanded = false }) {
                DropdownMenuItem(text = { Text("Turma inteira") }, onClick = {
                    studentId = -1L
                    studentPickerExpanded = false
                })
                classroomStudents.forEach { student ->
                    DropdownMenuItem(text = { Text(studentLabel(student)) }, onClick = {
                        studentId = student.id
                        studentPickerExpanded = false
                    })
                }
            }
        }
    }'''
assert s.count(old) == 1, f'Anchor count {s.count(old)}; refusing unsafe patch'
path.write_text(s.replace(old,new,1), encoding='utf-8')
print('Selector now chooses classroom students by ID and distinguishes duplicate names.')
