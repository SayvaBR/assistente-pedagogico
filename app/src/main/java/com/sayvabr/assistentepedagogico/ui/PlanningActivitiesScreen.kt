package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.LessonActivityV7
import com.sayvabr.assistentepedagogico.data.TeacherStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Real offline activity library; uses the Activity-owned store, not an additional database helper. */
@Composable
fun PlanningActivitiesScreen(
    store: TeacherStore,
    classroom: Classroom?,
    lessons: List<Lesson>,
    back: () -> Unit,
    onDirty: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var entries by remember(classroom?.id) { mutableStateOf(emptyList<LessonActivityV7.Activity>()) }
    var loading by remember(classroom?.id) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by rememberSaveable(classroom?.id) { mutableStateOf(false) }
    var editingId by rememberSaveable(classroom?.id) { mutableLongStateOf(-1L) }
    var title by rememberSaveable(classroom?.id) { mutableStateOf("") }
    var instructions by rememberSaveable(classroom?.id) { mutableStateOf("") }
    var duration by rememberSaveable(classroom?.id) { mutableStateOf("25") }
    var linkedLessonId by rememberSaveable(classroom?.id) { mutableLongStateOf(-1L) }
    // Restore both the draft and its unsaved status: TeacherApp's discard flag is transient.
    var fieldsDirty by rememberSaveable(classroom?.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    val room = classroom
    val availableLessons = lessons.filter { room != null && it.classroomId == room.id && !it.archived }
    val roomId = room?.id

    LaunchedEffect(fieldsDirty) {
        if (fieldsDirty) onDirty(true)
    }

    LaunchedEffect(roomId) {
        if (roomId == null) {
            entries = emptyList()
            loading = false
        } else {
            loading = true
            runCatching { withContext(Dispatchers.IO) { store.listActivities(roomId) } }
                .onSuccess { entries = it; error = null }
                .onFailure { error = it.message ?: "Não foi possível abrir as atividades." }
            loading = false
        }
    }

    fun markDirty() {
        fieldsDirty = true
        onDirty(true)
    }
    fun resetEditor() {
        editing = false
        editingId = -1L
        title = ""
        instructions = ""
        duration = "25"
        linkedLessonId = -1L
        fieldsDirty = false
        error = null
        onDirty(false)
    }
    fun startEditor(activity: LessonActivityV7.Activity? = null) {
        editing = true
        editingId = activity?.id ?: -1L
        title = activity?.title.orEmpty()
        instructions = activity?.instructions.orEmpty()
        duration = activity?.durationMinutes?.toString() ?: "25"
        linkedLessonId = activity?.lessonId ?: -1L
        fieldsDirty = false
        error = null
        onDirty(false)
    }
    fun save() {
        if (busy || roomId == null) return
        val value = LessonActivityV7.Input(
            classroomId = roomId,
            lessonId = linkedLessonId.takeIf { it > 0 },
            title = title,
            instructions = instructions,
            durationMinutes = duration.toIntOrNull() ?: 0,
        )
        busy = true
        error = null
        scope.launch {
            try {
                entries = withContext(Dispatchers.IO) {
                    if (editingId > 0) store.updateActivity(editingId, value)
                    else store.saveActivity(value)
                    store.listActivities(roomId)
                }
                resetEditor()
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível salvar a atividade."
            } finally {
                busy = false
            }
        }
    }
    fun duplicate(activityId: Long) {
        if (busy || roomId == null) return
        busy = true
        error = null
        scope.launch {
            try {
                entries = withContext(Dispatchers.IO) {
                    store.duplicateActivity(roomId, activityId)
                    store.listActivities(roomId)
                }
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível duplicar a atividade."
            } finally { busy = false }
        }
    }
    fun delete(activityId: Long) {
        if (busy || roomId == null) return
        busy = true
        error = null
        scope.launch {
            try {
                entries = withContext(Dispatchers.IO) {
                    store.deleteActivity(roomId, activityId)
                    store.listActivities(roomId)
                }
                confirmDelete = null
            } catch (e: Exception) {
                error = e.message ?: "Não foi possível excluir a atividade."
            } finally { busy = false }
        }
    }

    ApSectionHeading("Atividades", room?.let { "Biblioteca de ${it.name}" } ?: "Selecione uma turma no Planejamento")
    Spacer(Modifier.height(12.dp))
    ApRaisedButton("Voltar ao planejamento", onClick = back, secondary = true, glyph = ApGlyphKind.BACK)
    Spacer(Modifier.height(12.dp))
    if (room == null) {
        ApCard { Text("Cadastre ou selecione uma turma ativa antes de criar atividades.", color = ApColors.Navy) }
        return
    }
    error?.let {
        ApCard {
            Text("Atenção: $it", color = ApColors.Navy, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
    }
    if (loading) {
        CircularProgressIndicator(color = ApColors.Primary)
        return
    }
    if (editing) {
        ApCard {
            ApEyebrow(if (editingId > 0) "Editar atividade" else "Nova atividade", onPrimary = false)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(title, { if (title != it) { title = it; markDirty() } },
                modifier = Modifier.fillMaxWidth(), label = { Text("Título da atividade") }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(instructions, { if (instructions != it) { instructions = it; markDirty() } },
                modifier = Modifier.fillMaxWidth(), label = { Text("Descrição e orientações") }, minLines = 3)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(duration, { value ->
                val cleaned = value.filter(Char::isDigit)
                if (cleaned != duration) { duration = cleaned; markDirty() }
            }, modifier = Modifier.fillMaxWidth(), label = { Text("Duração estimada (minutos)") }, singleLine = true)
            Spacer(Modifier.height(14.dp))
            Text("Vincular a plano de aula (opcional)", fontWeight = FontWeight.ExtraBold, color = ApColors.Navy)
            Spacer(Modifier.height(8.dp))
            val choices: List<Pair<Long, String>> = listOf(-1L to "Atividade reutilizável, sem plano") +
                availableLessons.map { it.id to "${it.title} · ${it.date}" }
            choices.forEach { (id, label) ->
                val selected = linkedLessonId == id
                OutlinedButton(
                    onClick = { if (linkedLessonId != id) { linkedLessonId = id; markDirty() } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (selected) ApColors.Primary else ApPalette.Outline),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) ApColors.Primary else ApColors.White),
                ) { Text(label, color = if (selected) ApColors.White else ApColors.Navy, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            ApRaisedButton(if (editingId > 0) "Salvar alterações" else "Criar atividade", onClick = ::save,
                glyph = ApGlyphKind.CHECK, enabled = !busy)
            Spacer(Modifier.height(8.dp))
            ApRaisedButton("Cancelar edição", onClick = { confirmCancel = true }, secondary = true, enabled = !busy)
        }
    } else {
        ApRaisedButton("Nova atividade", onClick = { startEditor() }, glyph = ApGlyphKind.PLUS, enabled = !busy)
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) ApCard {
            Text("Nenhuma atividade nesta turma.", color = ApColors.Navy, fontWeight = FontWeight.Bold)
            Text("Crie uma atividade reutilizável ou vincule-a a um plano de aula.", color = ApColors.Navy)
        }
        entries.forEach { activity ->
            Spacer(Modifier.height(10.dp))
            ApCard {
                Text(activity.title, color = ApColors.Navy, fontWeight = FontWeight.Black)
                Text("${activity.durationMinutes} min · " +
                    (availableLessons.firstOrNull { it.id == activity.lessonId }?.title ?: "Sem plano ativo"),
                    color = ApColors.Pressed, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(activity.instructions, color = ApColors.Navy)
                Spacer(Modifier.height(10.dp))
                ApRaisedButton("Editar", onClick = { startEditor(activity) }, secondary = true, glyph = ApGlyphKind.EDIT, enabled = !busy)
                Spacer(Modifier.height(7.dp))
                ApRaisedButton("Duplicar", onClick = { duplicate(activity.id) }, secondary = true, glyph = ApGlyphKind.PLUS, enabled = !busy)
                Spacer(Modifier.height(7.dp))
                ApRaisedButton("Excluir atividade", onClick = { confirmDelete = activity.id }, secondary = true,
                    glyph = ApGlyphKind.TRASH, enabled = !busy)
            }
        }
    }
    if (confirmDelete != null) AlertDialog(
        onDismissRequest = { confirmDelete = null },
        title = { Text("Excluir esta atividade?") },
        text = { Text("A exclusão é definitiva e afeta apenas esta atividade. O plano de aula continuará salvo.") },
        confirmButton = { TextButton(onClick = { confirmDelete?.let(::delete) }, enabled = !busy) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } },
    )
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Descartar a edição?") },
        text = { Text("As alterações não salvas desta atividade serão perdidas.") },
        confirmButton = { TextButton(onClick = { confirmCancel = false; resetEditor() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Continuar") } },
    )
}
