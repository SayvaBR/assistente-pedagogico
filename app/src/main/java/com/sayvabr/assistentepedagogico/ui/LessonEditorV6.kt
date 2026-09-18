package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.LessonPlanV6

/** Professional offline lesson-plan editor. BNCC codes are stored locally; catalog validation comes in the next slice. */
@Composable
fun LessonEditorV6(
    classroom: Classroom,
    initialDay: String,
    initial: Lesson? = null,
    back: () -> Unit,
    save: (LessonPlanV6.Input) -> Unit,
    archive: (() -> Unit)? = null,
    onDirty: () -> Unit = {},
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var subject by rememberSaveable(initial?.id) { mutableStateOf(initial?.subject.orEmpty()) }
    var day by rememberSaveable(initial?.id) { mutableStateOf(initial?.date ?: initialDay) }
    var time by rememberSaveable(initial?.id) { mutableStateOf(initial?.time ?: "08:00") }
    var duration by rememberSaveable(initial?.id) { mutableStateOf((initial?.durationMinutes ?: 50).toString()) }
    var objective by rememberSaveable(initial?.id) { mutableStateOf(initial?.objective.orEmpty()) }
    var specific by rememberSaveable(initial?.id) { mutableStateOf(initial?.specificObjectives.orEmpty()) }
    var content by rememberSaveable(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    var bncc by rememberSaveable(initial?.id) { mutableStateOf(initial?.bnccCodes.orEmpty()) }
    var justification by rememberSaveable(initial?.id) { mutableStateOf(initial?.justification.orEmpty()) }
    var method by rememberSaveable(initial?.id) { mutableStateOf(initial?.method.orEmpty()) }
    var opening by rememberSaveable(initial?.id) { mutableStateOf(initial?.opening.orEmpty()) }
    var openingMinutes by rememberSaveable(initial?.id) { mutableStateOf((initial?.openingMinutes ?: 0).toString()) }
    var development by rememberSaveable(initial?.id) { mutableStateOf(initial?.development.orEmpty()) }
    var developmentMinutes by rememberSaveable(initial?.id) { mutableStateOf((initial?.developmentMinutes ?: 0).toString()) }
    var closing by rememberSaveable(initial?.id) { mutableStateOf(initial?.closing.orEmpty()) }
    var closingMinutes by rememberSaveable(initial?.id) { mutableStateOf((initial?.closingMinutes ?: 0).toString()) }
    var assessment by rememberSaveable(initial?.id) { mutableStateOf(initial?.assessment.orEmpty()) }
    var adaptations by rememberSaveable(initial?.id) { mutableStateOf(initial?.adaptations.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }

    fun field(label: String, value: String, multiline: Boolean = false, change: (String) -> Unit) {
        // local helper intentionally unused outside this composable; Compose calls below keep labels explicit.
    }
    fun update(old: String, next: String, setter: (String) -> Unit) {
        if (old != next) { setter(next); error = null; onDirty() }
    }
    @Composable fun TextField(label: String, value: String, multiline: Boolean = false, setter: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { update(value, it, setter) },
            label = { Text(label) },
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(9.dp))
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(if (initial == null) "Novo plano de aula" else "Editar plano de aula", color = ApColors.Navy, fontWeight = FontWeight.Black)
                Text(classroom.name, color = ApColors.Navy)
            }
            TextButton(onClick = back) { Text("Voltar") }
        }
        Spacer(Modifier.height(10.dp))
        ApCard {
            ApEyebrow("Identificação")
            TextField("Título da aula", title) { title = it }
            TextField("Componente curricular / disciplina", subject) { subject = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(day, { update(day, it) { v -> day = v } }, label = { Text("Data") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(time, { update(time, it) { v -> time = v } }, label = { Text("Início") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(duration, { update(duration, it) { v -> duration = v.filter(Char::isDigit) } }, label = { Text("Duração (min)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("O que vou ensinar")
            TextField("Objetivo geral", objective, true) { objective = it }
            TextField("Objetivos específicos (opcional)", specific, true) { specific = it }
            TextField("Conteúdo / objeto de conhecimento", content, true) { content = it }
            TextField("Habilidades BNCC — códigos separados por vírgula", bncc, true) { bncc = it }
            Text("Os códigos são armazenados agora; a seleção pelo catálogo oficial será conectada em uma próxima etapa.", color = ApColors.Navy)
            Spacer(Modifier.height(9.dp))
            TextField("Justificativa / contextualização (opcional)", justification, true) { justification = it }
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Como vou dar a aula")
            TextField("Metodologia / estratégia", method, true) { method = it }
            TextField("Abertura", opening, true) { opening = it }
            TextField("Tempo da abertura (min)", openingMinutes) { openingMinutes = it.filter(Char::isDigit) }
            TextField("Desenvolvimento", development, true) { development = it }
            TextField("Tempo do desenvolvimento (min)", developmentMinutes) { developmentMinutes = it.filter(Char::isDigit) }
            TextField("Fechamento", closing, true) { closing = it }
            TextField("Tempo do fechamento (min)", closingMinutes) { closingMinutes = it.filter(Char::isDigit) }
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Acompanhamento")
            TextField("Avaliação / evidências de aprendizagem (opcional)", assessment, true) { assessment = it }
            TextField("Adaptações e acessibilidade (opcional)", adaptations, true) { adaptations = it }
            error?.let { Text(it, color = ApColors.Navy, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            ApRaisedButton(if (initial == null) "Salvar plano de aula" else "Salvar alterações", onClick = {
                val input = LessonPlanV6.Input(
                    title, subject, day, time, duration.toIntOrNull() ?: 0, objective, specific, content, bncc,
                    justification, method, opening, openingMinutes.toIntOrNull() ?: 0, development,
                    developmentMinutes.toIntOrNull() ?: 0, closing, closingMinutes.toIntOrNull() ?: 0,
                    assessment, adaptations,
                )
                runCatching { LessonPlanV6.validated(input) }
                    .onSuccess(save)
                    .onFailure { error = it.message ?: "Revise os campos do plano." }
            }, glyph = ApGlyphKind.CHECK)
            if (archive != null) {
                Spacer(Modifier.height(10.dp))
                ApRaisedButton("Arquivar plano", onClick = { confirmArchive = true }, glyph = ApGlyphKind.FOLDER, secondary = true)
            }
        }
    }

    if (confirmArchive && archive != null) AlertDialog(
        onDismissRequest = { confirmArchive = false },
        title = { Text("Arquivar plano?") },
        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados.") },
        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
    )
}