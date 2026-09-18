package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.Appointment
import com.sayvabr.assistentepedagogico.data.AppointmentV5
import com.sayvabr.assistentepedagogico.data.Classroom
import java.time.LocalTime

/** Real offline appointment form; all writes go through the Activity-owned TeacherStore. */
@Composable
fun AppointmentEditorV5(
    initialDay: String,
    classes: List<Classroom>,
    initial: Appointment? = null,
    back: () -> Unit,
    save: (AppointmentV5.Input) -> Unit,
    delete: (() -> Unit)? = null,
    onDirty: () -> Unit = {},
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var day by rememberSaveable(initial?.id) { mutableStateOf(initial?.date ?: initialDay) }
    var start by rememberSaveable(initial?.id) { mutableStateOf(initial?.time ?: "08:00") }
    var end by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.endTime?.takeIf { it != initial.time } ?: "09:00")
    }
    var type by rememberSaveable(initial?.id) { mutableStateOf(initial?.type ?: "Outro") }
    var classroomId by rememberSaveable(initial?.id) { mutableStateOf(initial?.classroomId ?: -1L) }
    var typesExpanded by remember { mutableStateOf(false) }
    var classesExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val availableClasses = classes.filter { !it.archived }
    val selectedClass = availableClasses.firstOrNull { it.id == classroomId }

    fun update(previous: String, next: String, apply: (String) -> Unit) {
        if (previous != next) {
            apply(next)
            validationError = null
            onDirty()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (initial == null) "Novo compromisso" else "Editar compromisso",
                color = ApColors.Navy, fontWeight = FontWeight.Black)
            TextButton(onClick = back) { Text("Voltar") }
        }
        ApCard {
            OutlinedTextField(title, { update(title, it) { value -> title = value } },
                label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(day, { update(day, it) { value -> day = value } },
                label = { Text("Data (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(start, { update(start, it) { value -> start = value } },
                    label = { Text("Início HH:MM") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(end, { update(end, it) { value -> end = value } },
                    label = { Text("Fim HH:MM") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text("Categoria", color = ApColors.Navy, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { typesExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(type)
                }
                DropdownMenu(expanded = typesExpanded, onDismissRequest = { typesExpanded = false }) {
                    AppointmentV5.TYPES.sorted().forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            update(type, option) { value -> type = value }
                            typesExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Turma (opcional)", color = ApColors.Navy, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { classesExpanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(selectedClass?.let { "${it.name} · turma ${it.id}" } ?: "Geral / sem turma")
                }
                DropdownMenu(expanded = classesExpanded, onDismissRequest = { classesExpanded = false }) {
                    DropdownMenuItem(text = { Text("Geral / sem turma") }, onClick = {
                        if (classroomId != -1L) { classroomId = -1L; onDirty() }
                        classesExpanded = false
                    })
                    availableClasses.forEach { classroom ->
                        DropdownMenuItem(text = { Text("${classroom.name} · turma ${classroom.id}") }, onClick = {
                            if (classroomId != classroom.id) { classroomId = classroom.id; onDirty() }
                            classesExpanded = false
                        })
                    }
                }
            }
            if (initial != null && initial.endTime == initial.time) {
                Spacer(Modifier.height(8.dp))
                Text("Este compromisso antigo não tinha duração registrada. Confirme um horário final antes de salvar.",
                    color = ApColors.Navy)
            }
            validationError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = ApColors.Navy, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            ApRaisedButton(if (initial == null) "Salvar compromisso" else "Salvar alterações", onClick = {
                val input = AppointmentV5.Input(title, day, start, end, type, classroomId.takeIf { it > 0 })
                val checked = runCatching { AppointmentV5.validated(input) }
                checked.onSuccess { save(it) }
                checked.onFailure { validationError = it.message ?: "Verifique os campos informados." }
            }, glyph = ApGlyphKind.CHECK)
            if (delete != null) {
                Spacer(Modifier.height(10.dp))
                ApRaisedButton("Excluir compromisso", onClick = { confirmDelete = true },
                    glyph = ApGlyphKind.TRASH, secondary = true)
            }
        }
    }
    if (confirmDelete && delete != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Excluir compromisso?") },
        text = { Text("Essa operação remove permanentemente apenas o compromisso selecionado.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; delete() }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
    )
}
