package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayvabr.assistentepedagogico.data.BnccCatalog
import com.sayvabr.assistentepedagogico.data.Classroom
import com.sayvabr.assistentepedagogico.data.Lesson
import com.sayvabr.assistentepedagogico.data.LessonPlanShare
import com.sayvabr.assistentepedagogico.data.LessonPlanV6
import com.sayvabr.assistentepedagogico.data.LessonStatus

/** Professional offline editor; transitions travel through the existing Activity-owned commit callback. */
@Composable
fun LessonEditorV6(
    classroom: Classroom,
    initialDay: String,
    initial: Lesson? = null,
    back: () -> Unit,
    save: (LessonPlanV6.Input) -> Unit,
    archive: (() -> Unit)? = null,
    duplicate: (() -> Unit)? = null,
    onDirty: () -> Unit = {},
) {
    val context = LocalContext.current
    val editable = initial == null || initial.status == LessonStatus.DRAFT
    var fieldsDirty by rememberSaveable(initial?.id) { mutableStateOf(false) }
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
    var confirmDuplicate by remember { mutableStateOf(false) }
    var confirmStatus by remember { mutableStateOf<LessonStatus?>(null) }
    var showBnccPicker by rememberSaveable { mutableStateOf(false) }
    var catalogue by remember { mutableStateOf<BnccCatalog?>(null) }

    // TeacherApp's transient discard flag is reset when an Activity is recreated. Preserve the
    // editor flag and re-arm navigation protection after all saveable fields are restored.
    LaunchedEffect(fieldsDirty) {
        if (fieldsDirty) onDirty()
    }

    val predictedEnd = remember(time, duration) {
        runCatching { LessonPlanV6.endTime(time, duration.toIntOrNull() ?: 0) }.getOrNull()
    }
    val allocatedMinutes = listOf(openingMinutes, developmentMinutes, closingMinutes)
        .sumOf { it.toLongOrNull() ?: 0L }
    val totalMinutes = duration.toLongOrNull() ?: 0L

    fun editorInput() = LessonPlanV6.Input(
        title, subject, day, time, duration.toIntOrNull() ?: 0, objective, specific, content, bncc,
        justification, method, opening, openingMinutes.toIntOrNull() ?: 0, development,
        developmentMinutes.toIntOrNull() ?: 0, closing, closingMinutes.toIntOrNull() ?: 0,
        assessment, adaptations,
    )

    /** Saving and sharing validate precisely the same current fields and BNCC selection. */
    fun validatedEditorInput(): LessonPlanV6.Input {
        val valid = LessonPlanV6.validated(editorInput())
        if (valid.bnccCodes.isNotBlank()) {
            val verified = catalogue ?: BnccCatalog.load(context)
            val unknown = verified.unknownCodes(valid.bnccCodes)
            require(unknown.isEmpty()) {
                "Códigos BNCC não encontrados: ${unknown.joinToString(", ")}. Selecione habilidades verificadas."
            }
        }
        return valid
    }

    fun update(old: String, next: String, setter: (String) -> Unit) {
        if (editable && old != next) { setter(next); fieldsDirty = true; error = null; onDirty() }
    }
    @Composable fun TextField(label: String, value: String, multiline: Boolean = false, setter: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { update(value, it, setter) },
            label = { Text(label) },
            enabled = editable,
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(9.dp))
    }
    fun requestTransition(target: LessonStatus) {
        if (fieldsDirty) {
            error = "Salve ou descarte as alterações antes de mudar o estado. Nenhuma alteração foi perdida."
        } else confirmStatus = target
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(if (initial == null) "Novo plano de aula" else "Plano de aula", color = ApColors.Navy, fontWeight = FontWeight.Black)
                Text(classroom.name, color = ApColors.Navy)
            }
            TextButton(onClick = back) { Text("Voltar") }
        }
        Spacer(Modifier.height(10.dp))
        ApCard {
            ApEyebrow("Estado pedagógico", onPrimary = false)
            Text(initial?.status?.label ?: LessonStatus.DRAFT.label, color = ApColors.Navy, fontWeight = FontWeight.Black)
            if (initial == null) {
                Text("Salve o plano primeiro. Você poderá marcá-lo como pronto depois.", color = ApColors.Navy)
            } else {
                if (!editable) Text("Para alterar o conteúdo, reabra o plano como rascunho.", color = ApColors.Navy)
                when (initial.status) {
                    LessonStatus.DRAFT -> {
                        Spacer(Modifier.height(9.dp))
                        ApRaisedButton("Marcar como pronto", onClick = { requestTransition(LessonStatus.READY) }, glyph = ApGlyphKind.CHECK)
                    }
                    LessonStatus.READY -> {
                        Spacer(Modifier.height(9.dp))
                        ApRaisedButton("Marcar como concluído", onClick = { requestTransition(LessonStatus.COMPLETED) }, glyph = ApGlyphKind.CHECK)
                        Spacer(Modifier.height(9.dp))
                        ApRaisedButton("Reabrir como rascunho", onClick = { requestTransition(LessonStatus.DRAFT) }, glyph = ApGlyphKind.EDIT, secondary = true)
                    }
                    LessonStatus.COMPLETED -> {
                        Spacer(Modifier.height(9.dp))
                        ApRaisedButton("Reabrir como pronto", onClick = { requestTransition(LessonStatus.READY) }, glyph = ApGlyphKind.RESTORE, secondary = true)
                    }
                    LessonStatus.ARCHIVED -> Text("Restaure este plano na lista de arquivados.", color = ApColors.Navy)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Identificação", onPrimary = false)
            TextField("Título da aula", title) { title = it }
            TextField("Componente curricular / disciplina", subject) { subject = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(day, { update(day, it) { v -> day = v } }, label = { Text("Data") }, enabled = editable, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(time, { update(time, it) { v -> time = v } }, label = { Text("Início") }, enabled = editable, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(duration, { update(duration, it) { v -> duration = v.filter(Char::isDigit) } }, label = { Text("Duração (min)") }, enabled = editable, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                if (predictedEnd != null) "Término previsto: $predictedEnd" else "Informe início (HH:MM) e duração válida para calcular o término.",
                color = ApColors.Navy,
                fontWeight = if (predictedEnd != null) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("O que vou ensinar", onPrimary = false)
            TextField("Objetivo geral", objective, true) { objective = it }
            TextField("Objetivos específicos (opcional)", specific, true) { specific = it }
            TextField("Conteúdo / objeto de conhecimento", content, true) { content = it }
            OutlinedTextField(
                value = bncc,
                onValueChange = {},
                readOnly = true,
                label = { Text("Habilidades BNCC selecionadas") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (editable) ApRaisedButton("Buscar e selecionar habilidades BNCC", onClick = {
                runCatching { catalogue ?: BnccCatalog.load(context) }
                    .onSuccess { catalogue = it; error = null; showBnccPicker = true }
                    .onFailure { error = "Catálogo BNCC offline indisponível: ${it.message ?: "verifique a instalação."}" }
            }, glyph = ApGlyphKind.CHECK, secondary = true)
            Text("Catálogo offline de terceiros em auditoria contra os documentos oficiais da BNCC.", color = ApColors.Navy)
            Spacer(Modifier.height(9.dp))
            TextField("Justificativa / contextualização (opcional)", justification, true) { justification = it }
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Como vou dar a aula", onPrimary = false)
            TextField("Metodologia / estratégia", method, true) { method = it }
            TextField("Abertura", opening, true) { opening = it }
            TextField("Tempo da abertura (min)", openingMinutes) { openingMinutes = it.filter(Char::isDigit) }
            TextField("Desenvolvimento", development, true) { development = it }
            TextField("Tempo do desenvolvimento (min)", developmentMinutes) { developmentMinutes = it.filter(Char::isDigit) }
            TextField("Fechamento", closing, true) { closing = it }
            TextField("Tempo do fechamento (min)", closingMinutes) { closingMinutes = it.filter(Char::isDigit) }
            Text(
                "Momentos distribuídos: $allocatedMinutes de $totalMinutes min" +
                    if (allocatedMinutes > totalMinutes) " — ajuste os tempos antes de salvar." else "",
                color = ApColors.Navy,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Acompanhamento", onPrimary = false)
            TextField("Avaliação / evidências de aprendizagem (opcional)", assessment, true) { assessment = it }
            TextField("Adaptações e acessibilidade (opcional)", adaptations, true) { adaptations = it }
            error?.let { Text(it, color = ApColors.Navy, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            if (editable) {
                ApRaisedButton(if (initial == null) "Salvar plano de aula" else "Salvar alterações", onClick = {
                    runCatching { save(validatedEditorInput()) }
                        .onFailure { error = it.message ?: "Revise os campos do plano." }
                }, glyph = ApGlyphKind.CHECK)
                Spacer(Modifier.height(10.dp))
            }
            ApRaisedButton("Compartilhar prévia do plano", onClick = {
                runCatching {
                    val preview = LessonPlanShare.asPlainText(validatedEditorInput(), classroom.name)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Plano de aula — ${title.trim()}")
                        putExtra(Intent.EXTRA_TEXT, preview)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartilhar prévia do plano"))
                    error = null
                }.onFailure { error = it.message ?: "Não foi possível compartilhar este plano." }
            }, glyph = ApGlyphKind.DOCUMENT, secondary = true)
            Text("A prévia contém os campos atuais. Compartilhar não salva alterações no aplicativo.", color = ApColors.Navy)
            if (duplicate != null) {
                Spacer(Modifier.height(10.dp))
                ApRaisedButton("Duplicar plano e atividades", onClick = { confirmDuplicate = true }, glyph = ApGlyphKind.PLUS, secondary = true)
            }
            if (archive != null) {
                Spacer(Modifier.height(10.dp))
                ApRaisedButton("Arquivar plano", onClick = { confirmArchive = true }, glyph = ApGlyphKind.FOLDER, secondary = true)
            }
        }
    }

    if (showBnccPicker && catalogue != null && editable) BnccPicker(
        catalog = requireNotNull(catalogue),
        initialCodes = BnccCatalog.parseCodes(bncc),
        onConfirm = { codes ->
            update(bncc, codes.joinToString(", ")) { bncc = it }
            showBnccPicker = false
        },
        onDismiss = { showBnccPicker = false },
    )

    confirmStatus?.let { target -> AlertDialog(
        onDismissRequest = { confirmStatus = null },
        title = { Text("Alterar estado para ${target.label}?") },
        text = { Text("Esta ação será salva no banco local. Alterações não salvas precisam ser salvas ou descartadas primeiro.") },
        confirmButton = { TextButton(onClick = {
            confirmStatus = null
            if (fieldsDirty) error = "Salve ou descarte as alterações antes de mudar o estado."
            else save(editorInput().copy(statusTransition = target))
        }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { confirmStatus = null }) { Text("Cancelar") } },
    ) }
    if (confirmArchive && archive != null) AlertDialog(
        onDismissRequest = { confirmArchive = false },
        title = { Text("Arquivar plano?") },
        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados. Alterações ainda não salvas não serão incluídas.") },
        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } },
    )
    if (confirmDuplicate && duplicate != null) AlertDialog(
        onDismissRequest = { confirmDuplicate = false },
        title = { Text("Duplicar plano e atividades?") },
        text = { Text("Uma cópia independente do plano e de suas atividades vinculadas será criada na mesma turma. Alterações ainda não salvas nesta tela não serão copiadas.") },
        confirmButton = { TextButton(onClick = { confirmDuplicate = false; duplicate() }) { Text("Duplicar") } },
        dismissButton = { TextButton(onClick = { confirmDuplicate = false }) { Text("Cancelar") } },
    )
}
