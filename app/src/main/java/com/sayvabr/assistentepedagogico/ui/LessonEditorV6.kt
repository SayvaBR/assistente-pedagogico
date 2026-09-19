package com.sayvabr.assistentepedagogico.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The real offline editor composes canonical fields in the order stored in SQLite v9. */
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
    val store = LocalTeacherStore.current
    val scope = rememberCoroutineScope()
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
    var layoutJson by rememberSaveable(initial?.id) {
        mutableStateOf(if (initial == null) PlanLayoutV9.encode(PlanComposition.standard()) else "")
    }
    var templates by remember { mutableStateOf(emptyList<Pair<Long, PlanTemplate>>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    var confirmDuplicate by remember { mutableStateOf(false) }
    var confirmStatus by remember { mutableStateOf<LessonStatus?>(null) }
    var showBnccPicker by rememberSaveable { mutableStateOf(false) }
    var catalogue by remember { mutableStateOf<BnccCatalog?>(null) }
    val layout = remember(layoutJson) {
        if (layoutJson.isBlank()) PlanComposition.standard() else PlanLayoutV9.decode(layoutJson)
    }

    LaunchedEffect(store, initial?.id) {
        try {
            val loaded = withContext(Dispatchers.IO) {
                store.planTemplates() to initial?.let { store.lessonComposition(classroom.id, it.id) }
            }
            templates = loaded.first
            // Rotation restores the user's unsaved layout; never overwrite it with the persisted layout.
            if (layoutJson.isBlank() && loaded.second != null) layoutJson = PlanLayoutV9.encode(loaded.second!!)
        } catch (e: Exception) { error = e.message ?: "Não foi possível carregar a estrutura do plano." }
    }
    LaunchedEffect(showBnccPicker) {
        if (showBnccPicker && catalogue == null) {
            runCatching { withContext(Dispatchers.IO) { BnccCatalog.load(context) } }
                .onSuccess { catalogue = it }
                .onFailure { error = "Catálogo BNCC offline indisponível: ${it.message ?: "verifique a instalação."}"; showBnccPicker = false }
        }
    }
    // TeacherApp's transient discard flag resets on Activity recreation. Re-arm after restoration.
    LaunchedEffect(fieldsDirty) { if (fieldsDirty) onDirty() }

    val predictedEnd = remember(time, duration) {
        runCatching { LessonPlanV6.endTime(time, duration.toIntOrNull() ?: 0) }.getOrNull()
    }
    val allocatedMinutes = listOf(openingMinutes, developmentMinutes, closingMinutes)
        .sumOf { it.toLongOrNull() ?: 0L }
    val totalMinutes = duration.toLongOrNull() ?: 0L
    val timingFraction = if (totalMinutes > 0L) (allocatedMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f) else 0f

    fun editorInput() = LessonPlanV6.Input(
        title, subject, day, time, duration.toIntOrNull() ?: 0, objective, specific, content, bncc,
        justification, method, opening, openingMinutes.toIntOrNull() ?: 0, development,
        developmentMinutes.toIntOrNull() ?: 0, closing, closingMinutes.toIntOrNull() ?: 0,
        assessment, adaptations,
    )

    /** Saving and sharing validate the same canonical fields and selected BNCC codes. */
    fun validatedEditorInput(): LessonPlanV6.Input {
        require(layoutJson.isNotBlank()) { "A estrutura do plano ainda não foi carregada." }
        val valid = LessonPlanV6.validated(editorInput())
        if (valid.bnccCodes.isNotBlank()) {
            val verified = catalogue ?: BnccCatalog.load(context)
            val unknown = verified.unknownCodes(valid.bnccCodes)
            require(unknown.isEmpty()) {
                "Códigos BNCC não encontrados: ${unknown.joinToString(", ")}. Selecione habilidades disponíveis no catálogo."
            }
        }
        return valid
    }

    fun update(old: String, next: String, setter: (String) -> Unit) {
        if (editable && old != next) { setter(next); fieldsDirty = true; error = null; onDirty() }
    }
    fun changeLayout(next: PlanComposition) {
        if (editable && layoutJson.isNotBlank() && next != layout) {
            runCatching { PlanLayoutV9.encode(next) }
                .onSuccess { layoutJson = it; fieldsDirty = true; error = null; onDirty() }
                .onFailure { error = it.message ?: "Não foi possível alterar a seção." }
        }
    }
    @Composable fun TextField(label: String, value: String, multiline: Boolean = false, setter: (String) -> Unit) {
        OutlinedTextField(value = value, onValueChange = { update(value, it, setter) },
            label = { Text(label) }, enabled = editable, singleLine = !multiline,
            minLines = if (multiline) 3 else 1, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(9.dp))
    }
    fun requestTransition(target: LessonStatus) {
        if (fieldsDirty) error = "Salve ou descarte as alterações antes de mudar o estado. Nenhuma alteração foi perdida."
        else confirmStatus = target
    }

    Column(Modifier.fillMaxWidth()) {
        PlanningTopBar(if (initial == null) "Novo plano de aula" else "Plano de aula",
            subtitle = classroom.name, back = back)
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Estado pedagógico", onPrimary = false)
            Spacer(Modifier.height(6.dp))
            Surface(color = ApPalette.LightSurface, shape = RoundedCornerShape(50.dp)) {
                Text(initial?.status?.label ?: LessonStatus.DRAFT.label,
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = ApPalette.Pressed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(7.dp))
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
        if (layoutJson.isBlank()) {
            ApCard { Text("Carregando a organização do plano. Seus registros estão preservados.", color = ApColors.Navy) }
        } else {
            PlanComposerPanel(layout = layout, enabled = editable, templates = templates,
                onChange = { changeLayout(it) },
                onSaveTemplate = { name ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                store.createPlanTemplate(layout.saveAsTemplate(name))
                                store.planTemplates()
                            }
                        }.onSuccess { templates = it; error = null }
                            .onFailure { error = it.message ?: "Não foi possível salvar o modelo." }
                    }
                },
                onRemoveTemplate = { id ->
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { store.deletePlanTemplate(id); store.planTemplates() } }
                            .onSuccess { templates = it; error = null }
                            .onFailure { error = it.message ?: "Não foi possível excluir o modelo." }
                    }
                },
                sectionContent = { block ->
                    when (block.kind) {
                        PlanBlockKind.IDENTIFICATION -> {
                            TextField("Título da aula", title) { title = it }
                            TextField("Componente curricular / disciplina", subject) { subject = it }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(day, { update(day, it) { v -> day = v } },
                                    label = { Text("Data") }, enabled = editable, singleLine = true,
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                                OutlinedTextField(time, { update(time, it) { v -> time = v } },
                                    label = { Text("Início") }, enabled = editable, singleLine = true,
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            }
                            Spacer(Modifier.height(9.dp))
                            OutlinedTextField(duration, { update(duration, it) { v -> duration = v.filter(Char::isDigit) } },
                                label = { Text("Duração (min)") }, enabled = editable, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(if (predictedEnd != null) "Término previsto: $predictedEnd"
                                else "Informe início (HH:MM) e duração válida para calcular o término.",
                                color = ApColors.Navy,
                                fontWeight = if (predictedEnd != null) FontWeight.Bold else FontWeight.Normal)
                        }
                        PlanBlockKind.OBJECTIVES -> {
                            TextField("Objetivo geral", objective, true) { objective = it }
                            TextField("Objetivos específicos (opcional)", specific, true) { specific = it }
                        }
                        PlanBlockKind.CONTENT -> TextField("Conteúdo / objeto de conhecimento", content, true) { content = it }
                        PlanBlockKind.BNCC -> {
                            OutlinedTextField(value = bncc, onValueChange = {}, readOnly = true,
                                label = { Text("Habilidades BNCC selecionadas") }, minLines = 2,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                            Spacer(Modifier.height(8.dp))
                            if (editable) ApRaisedButton("Buscar e selecionar habilidades BNCC", onClick = {
                                showBnccPicker = true
                            }, glyph = ApGlyphKind.CHECK, secondary = true)
                            Text("Habilidades de referência disponíveis offline. Use-as como apoio ao planejamento; consulte os documentos oficiais quando precisar de conferência normativa.", color = ApColors.Navy)
                        }
                        PlanBlockKind.CONTEXT -> TextField("Justificativa / contextualização (opcional)", justification, true) { justification = it }
                        PlanBlockKind.METHODOLOGY -> TextField("Metodologia / estratégia", method, true) { method = it }
                        PlanBlockKind.OPENING -> {
                            TextField("Abertura", opening, true) { opening = it }
                            TextField("Tempo da abertura (min)", openingMinutes) { openingMinutes = it.filter(Char::isDigit) }
                        }
                        PlanBlockKind.DEVELOPMENT -> {
                            TextField("Desenvolvimento", development, true) { development = it }
                            TextField("Tempo do desenvolvimento (min)", developmentMinutes) { developmentMinutes = it.filter(Char::isDigit) }
                        }
                        PlanBlockKind.CLOSING -> {
                            TextField("Fechamento", closing, true) { closing = it }
                            TextField("Tempo do fechamento (min)", closingMinutes) { closingMinutes = it.filter(Char::isDigit) }
                        }
                        PlanBlockKind.ASSESSMENT -> TextField("Avaliação / evidências de aprendizagem (opcional)", assessment, true) { assessment = it }
                        PlanBlockKind.ADAPTATIONS -> TextField("Adaptações e acessibilidade (opcional)", adaptations, true) { adaptations = it }
                        PlanBlockKind.RESOURCES -> Text("Para descrever recursos, adicione uma seção personalizada; este campo ainda não pertence ao registro canônico.", color = ApColors.Navy)
                        PlanBlockKind.CUSTOM -> Unit // PlanComposerPanel renders the custom text directly.
                    }
                })
        }
        Spacer(Modifier.height(12.dp))
        ApCard {
            ApEyebrow("Concluir e compartilhar", onPrimary = false)
            Spacer(Modifier.height(8.dp))
            Text("Momentos distribuídos: $allocatedMinutes de $totalMinutes min" +
                if (allocatedMinutes > totalMinutes) " — ajuste os tempos antes de salvar." else "",
                color = ApColors.Navy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Box(Modifier.fillMaxWidth().height(9.dp).background(ApPalette.LightSurface, RoundedCornerShape(50.dp))) {
                Box(Modifier.fillMaxWidth(timingFraction).height(9.dp)
                    .background(if (allocatedMinutes > totalMinutes) ApPalette.Navy else ApPalette.Primary,
                        RoundedCornerShape(50.dp)))
            }
            Spacer(Modifier.height(9.dp))
            Text("A barra representa apenas a distribuição dos minutos, não a qualidade pedagógica do plano.",
                color = ApPalette.Navy.copy(alpha = .75f), fontSize = 11.sp)
            error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = ApColors.Navy, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(11.dp))
            if (editable) {
                ApRaisedButton(if (initial == null) "Salvar plano de aula" else "Salvar alterações", onClick = {
                    runCatching { save(validatedEditorInput().copy(composition = layout)) }
                        .onFailure { error = it.message ?: "Revise os campos do plano." }
                }, glyph = ApGlyphKind.CHECK, enabled = layoutJson.isNotBlank())
                Spacer(Modifier.height(10.dp))
            }
            ApRaisedButton("Compartilhar prévia do plano", onClick = {
                runCatching {
                    val preview = LessonPlanShare.asPlainText(validatedEditorInput(), classroom.name, layout)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Plano de aula — ${title.trim()}")
                        putExtra(Intent.EXTRA_TEXT, preview)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartilhar prévia do plano"))
                    error = null
                }.onFailure { error = it.message ?: "Não foi possível compartilhar este plano." }
            }, glyph = ApGlyphKind.DOCUMENT, secondary = true, enabled = layoutJson.isNotBlank())
            Text("A prévia respeita a ordem das seções atuais. Compartilhar não salva alterações no aplicativo.", color = ApColors.Navy)
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
        catalog = requireNotNull(catalogue), initialCodes = BnccCatalog.parseCodes(bncc),
        onConfirm = { codes ->
            update(bncc, codes.joinToString(", ")) { bncc = it }
            showBnccPicker = false
        }, onDismiss = { showBnccPicker = false })

    confirmStatus?.let { target -> AlertDialog(
        onDismissRequest = { confirmStatus = null },
        title = { Text("Alterar estado para ${target.label}?") },
        text = { Text("Esta ação será salva no banco local. Alterações não salvas precisam ser salvas ou descartadas primeiro.") },
        confirmButton = { TextButton(onClick = {
            confirmStatus = null
            if (fieldsDirty) error = "Salve ou descarte as alterações antes de mudar o estado."
            else save(editorInput().copy(statusTransition = target))
        }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { confirmStatus = null }) { Text("Cancelar") } }) }
    if (confirmArchive && archive != null) AlertDialog(
        onDismissRequest = { confirmArchive = false },
        title = { Text("Arquivar plano?") },
        text = { Text("O plano será preservado e poderá ser restaurado em Planejamento > Arquivados. Alterações ainda não salvas não serão incluídas.") },
        confirmButton = { TextButton(onClick = { confirmArchive = false; archive() }) { Text("Arquivar") } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } })
    if (confirmDuplicate && duplicate != null) AlertDialog(
        onDismissRequest = { confirmDuplicate = false },
        title = { Text("Duplicar plano e atividades?") },
        text = { Text("Uma cópia independente do plano e de suas atividades vinculadas será criada na mesma turma. Alterações ainda não salvas nesta tela não serão copiadas.") },
        confirmButton = { TextButton(onClick = { confirmDuplicate = false; duplicate() }) { Text("Duplicar") } },
        dismissButton = { TextButton(onClick = { confirmDuplicate = false }) { Text("Cancelar") } })
}
