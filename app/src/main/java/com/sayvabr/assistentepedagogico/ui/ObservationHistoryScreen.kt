package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.TeacherSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Shows ALL classroom observations, not only the five recent entries in class details.
 * No cross-class note or student is shown; opening a record is delegated by its stable ID.
 */
@Composable
fun ObservationHistoryScreen(
    data: TeacherSnapshot,
    classroom: Classroom,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
    initialStudentFilter: Long? = null,
) {
    var query by rememberSaveable(classroom.id) { mutableStateOf("") }
    var category by rememberSaveable(classroom.id) { mutableStateOf("Todas") }
    var studentFilter by rememberSaveable(classroom.id, initialStudentFilter) {
        mutableLongStateOf(initialStudentFilter ?: -1L)
    }
    val students = data.students.filter { it.classroomId == classroom.id }
    val names = students.associate { it.id to it.name }
    val notes = data.observations.filter { it.classroomId == classroom.id }
    val visible = notes.filter { note ->
        (category == "Todas" || note.kind == category) &&
            (studentFilter == -1L || note.studentId == studentFilter) &&
            (query.isBlank() || note.body.contains(query.trim(), ignoreCase = true) ||
                note.kind.contains(query.trim(), ignoreCase = true) ||
                note.studentId?.let { names[it] }?.contains(query.trim(), ignoreCase = true) == true)
    }.sortedWith(compareByDescending<com.sayvabr.assistentepedagogico.data.Observation> { it.date }.thenByDescending { it.id })

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp).clickable(onClick = onBack)
                .semantics { contentDescription = "Voltar à turma" },
            shape = RoundedCornerShape(14.dp), color = ApColors.White,
            border = BorderStroke(1.dp, Color(0xFFCDE8F8)),
        ) { Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.BACK, color = ApColors.Primary) } }
        Spacer(Modifier.width(12.dp))
        Column {
            ApEyebrow(classroom.name)
            Text("Histórico de registros", fontSize = 23.sp, fontWeight = FontWeight.Black, color = ApColors.Navy)
        }
    }
    Spacer(Modifier.height(18.dp))
    Surface(shape = RoundedCornerShape(22.dp), color = ApColors.Primary, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(19.dp)) {
            Text("REGISTROS PEDAGÓGICOS", color = ApColors.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text("${notes.size} ${if (notes.size == 1) "observação salva" else "observações salvas"}",
                color = ApColors.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text("Consulte, revise e acompanhe os registros desta turma.", color = ApColors.White, fontSize = 13.sp)
            Spacer(Modifier.height(15.dp))
            ApRaisedButton("Nova observação", onClick = onNew, glyph = ApGlyphKind.EDIT, secondary = true)
        }
    }
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = query, onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text("Buscar por descrição ou aluno") },
        leadingIcon = { ApGlyph(ApGlyphKind.SEARCH, Modifier.size(21.dp), color = ApColors.Pressed) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ApColors.Primary,
            unfocusedBorderColor = Color(0xFFCDE8F8),
            focusedContainerColor = ApColors.White,
            unfocusedContainerColor = ApColors.White,
        ),
    )
    Spacer(Modifier.height(11.dp))
    Text("Tipo", color = ApColors.Navy, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("Todas", "Participação", "Aprendizagem", "Comportamento", "Outro").forEach { value ->
            val selected = category == value
            Surface(
                modifier = Modifier.clickable { category = value },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) ApColors.Primary else ApColors.White,
                border = BorderStroke(1.dp, if (selected) ApColors.Primary else Color(0xFFCDE8F8)),
            ) {
                Text(value, Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    color = if (selected) ApColors.White else ApColors.Navy,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(13.dp))
    if (students.isNotEmpty()) {
        Text("Aluno", color = ApColors.Navy, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val filters = listOf(-1L to "Todos") + students.map { it.id to it.name }
            filters.forEach { (id, label) ->
                val selected = studentFilter == id
                Surface(modifier = Modifier.clickable { studentFilter = id },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) ApColors.Primary else ApColors.White,
                    border = BorderStroke(1.dp, if (selected) ApColors.Primary else Color(0xFFCDE8F8))) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        color = if (selected) ApColors.White else ApColors.Navy,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(13.dp))
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Observações", fontSize = 19.sp, fontWeight = FontWeight.Black, color = ApColors.Navy, modifier = Modifier.weight(1f))
        Text("${visible.size} exibidas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ApColors.Pressed)
    }
    Spacer(Modifier.height(11.dp))
    if (visible.isEmpty()) {
        ApCard {
            Text(if (notes.isEmpty()) "Nenhuma observação ainda" else "Nenhum registro encontrado",
                fontWeight = FontWeight.Black, fontSize = 18.sp, color = ApColors.Navy)
            Spacer(Modifier.height(7.dp))
            Text(if (notes.isEmpty()) "Crie a primeira observação desta turma." else "Ajuste os filtros ou a palavra pesquisada.",
                color = ApColors.Navy, fontSize = 14.sp)
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(13.dp))
                ApRaisedButton("Limpar filtros", secondary = true, onClick = {
                    query = ""; category = "Todas"; studentFilter = -1L
                })
            }
        }
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    visible.forEach { note ->
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onOpen(note.id) }
                .semantics { contentDescription = "Abrir observação de ${note.kind}, ${note.date}" },
            color = ApColors.White,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFCDE8F8)),
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(45.dp), contentAlignment = Alignment.Center) {
                    ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(30.dp), color = ApColors.Pressed)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(note.kind, color = ApColors.Pressed, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(runCatching { LocalDate.parse(note.date).format(dateFormat) }.getOrDefault(note.date),
                        color = ApColors.Navy, fontSize = 12.sp)
                    note.studentId?.let { names[it] }?.let { name ->
                        Spacer(Modifier.height(3.dp))
                        Text(name, color = ApColors.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(note.body, color = ApColors.Navy, fontSize = 14.sp,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("Toque para consultar ou editar", color = ApColors.Pressed,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
    }
}
