package com.sayvabr.assistentepedagogico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayvabr.assistentepedagogico.data.PlanBlock
import com.sayvabr.assistentepedagogico.data.PlanBlockKind
import com.sayvabr.assistentepedagogico.data.PlanComposition
import com.sayvabr.assistentepedagogico.data.PlanTemplate
import java.util.UUID

/** Presentation-only composer. Persisted section IDs, order, data and template semantics remain canonical. */
@Composable
fun PlanComposerPanel(
    layout: PlanComposition,
    enabled: Boolean,
    templates: List<Pair<Long, PlanTemplate>>,
    onChange: (PlanComposition) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onRemoveTemplate: (Long) -> Unit,
    sectionContent: @Composable (PlanBlock) -> Unit,
) {
    var addMenu by remember { mutableStateOf(false) }
    var modelsExpanded by rememberSaveable { mutableStateOf(false) }
    var templateName by rememberSaveable { mutableStateOf("") }
    var pendingTemplate by remember { mutableStateOf<Pair<Long, PlanTemplate>?>(null) }
    var pendingRemoval by remember { mutableStateOf<Long?>(null) }
    var pendingSectionRemoval by remember { mutableStateOf<PlanBlock?>(null) }
    var pendingRename by remember { mutableStateOf<PlanBlock?>(null) }
    var renameTitle by rememberSaveable { mutableStateOf("") }
    val mandatory = setOf(PlanBlockKind.IDENTIFICATION, PlanBlockKind.OBJECTIVES,
        PlanBlockKind.CONTENT, PlanBlockKind.METHODOLOGY)
    val addable = PlanBlockKind.entries.filter { kind ->
        kind != PlanBlockKind.IDENTIFICATION && kind != PlanBlockKind.CUSTOM &&
            kind != PlanBlockKind.RESOURCES && layout.blocks.none { it.kind == kind }
    }

    Surface(Modifier.fillMaxWidth(), color = ApPalette.Navy, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(Modifier.size(52.dp), color = ApPalette.Primary, shape = RoundedCornerShape(17.dp)) {
                    Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.DOCUMENT, Modifier.size(27.dp), Color.White) }
                }
                Column(Modifier.weight(1f)) {
                    Text("SEU PLANO DE AULA", color = ApPalette.LightSurface, fontSize = 10.sp,
                        letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(3.dp))
                    Text("Estrutura do plano", color = Color.White, fontSize = 22.sp,
                        lineHeight = 27.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${layout.blocks.size} seções organizadas por você", color = Color.White,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Reorganize os blocos e escreva no seu ritmo. As informações essenciais continuam protegidas.",
                color = Color.White.copy(alpha = .92f), fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(16.dp))
            Box {
                ApRaisedButton("Adicionar seção", enabled = enabled && layout.blocks.size < 40,
                    glyph = ApGlyphKind.PLUS, onClick = { addMenu = true }, secondary = true)
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(text = { Text("Seção personalizada") }, onClick = {
                        addMenu = false
                        onChange(layout.add(PlanBlock("custom_${UUID.randomUUID().toString().replace("-", "")}",
                            PlanBlockKind.CUSTOM, "Nova seção")))
                    })
                    addable.forEach { kind ->
                        DropdownMenuItem(text = { Text(kind.defaultTitle) }, onClick = {
                            addMenu = false
                            onChange(layout.add(PlanBlock(kind.name.lowercase(), kind)))
                        })
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Surface(Modifier.fillMaxWidth(), color = ApPalette.White, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ApPalette.Outline)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { modelsExpanded = !modelsExpanded }
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Surface(Modifier.size(43.dp), color = ApPalette.LightSurface, shape = RoundedCornerShape(13.dp)) {
                    Box(contentAlignment = Alignment.Center) { ApGlyph(ApGlyphKind.FOLDER, Modifier.size(23.dp), ApPalette.Primary) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Meus modelos", color = ApPalette.Navy, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("${templates.size} modelos · reutilize a estrutura", color = ApPalette.Navy.copy(alpha = .75f), fontSize = 12.sp)
                }
                Text(if (modelsExpanded) "Fechar" else "Abrir", color = ApPalette.Pressed,
                    fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            if (modelsExpanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Text("Salve a estrutura para reutilizar em outras aulas.", color = ApPalette.Navy,
                        fontSize = 12.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(templateName, { templateName = it.take(80) },
                        label = { Text("Nome do modelo da escola") }, enabled = enabled,
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp))
                    Spacer(Modifier.height(9.dp))
                    ApRaisedButton("Salvar estrutura como modelo", onClick = { onSaveTemplate(templateName) },
                        enabled = enabled && templateName.trim().length >= 2,
                        glyph = ApGlyphKind.CHECK, secondary = true)
                    templates.forEach { template ->
                        Spacer(Modifier.height(12.dp))
                        Surface(color = ApPalette.LightSurface, shape = RoundedCornerShape(15.dp)) {
                            Column(Modifier.fillMaxWidth().padding(11.dp)) {
                                Text(template.second.name, color = ApPalette.Navy, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TextButton(onClick = { pendingTemplate = template }, enabled = enabled) {
                                        Text("Aplicar", color = ApPalette.Pressed)
                                    }
                                    TextButton(onClick = { pendingRemoval = template.first }, enabled = enabled) {
                                        Text("Excluir modelo", color = ApPalette.Pressed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Modelos não copiam o texto da aula. Remover uma seção opcional não apaga os demais campos do plano.",
        color = ApPalette.Navy.copy(alpha = .75f), fontSize = 11.sp, lineHeight = 17.sp)

    layout.blocks.forEachIndexed { index, block ->
        Spacer(Modifier.height(12.dp))
        ApCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.size(46.dp), color = ApPalette.LightSurface, shape = RoundedCornerShape(15.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        val symbol = when (block.kind) {
                            PlanBlockKind.BNCC -> ApGlyphKind.DOCUMENT
                            PlanBlockKind.OBJECTIVES -> ApGlyphKind.CHECK
                            PlanBlockKind.RESOURCES -> ApGlyphKind.FOLDER
                            PlanBlockKind.CUSTOM -> ApGlyphKind.EDIT
                            else -> ApGlyphKind.NOTE
                        }
                        ApGlyph(symbol, Modifier.size(24.dp), ApPalette.Primary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("ETAPA ${index + 1} / ${layout.blocks.size}", color = ApPalette.Pressed,
                        fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = .6.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(block.title, color = ApPalette.Navy, fontWeight = FontWeight.Black,
                        fontSize = 17.sp, lineHeight = 22.sp)
                }
                if (block.kind in mandatory) Surface(color = ApPalette.LightSurface,
                    shape = RoundedCornerShape(50.dp)) {
                    Text("Essencial", Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = ApPalette.Pressed, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ApPalette.Outline))
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { renameTitle = block.title; pendingRename = block }, enabled = enabled) {
                ApGlyph(ApGlyphKind.EDIT, Modifier.size(17.dp), ApPalette.Primary)
                Spacer(Modifier.width(7.dp))
                Text("Renomear seção", color = ApPalette.Pressed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = { onChange(layout.move(block.id, -1)) }, enabled = enabled && index > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("↑ Subir", fontSize = 12.sp, color = ApPalette.Pressed)
                }
                TextButton(onClick = { onChange(layout.move(block.id, 1)) },
                    enabled = enabled && index < layout.blocks.lastIndex,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("↓ Descer", fontSize = 12.sp, color = ApPalette.Pressed)
                }
                if (block.kind !in mandatory) TextButton(onClick = { pendingSectionRemoval = block }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Remover", fontSize = 12.sp, color = ApPalette.Pressed)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (block.kind == PlanBlockKind.CUSTOM) {
                OutlinedTextField(block.body, { value ->
                    if (enabled && value.length <= 20_000) onChange(layout.update(block.copy(body = value)))
                }, label = { Text("Texto da seção personalizada") }, enabled = enabled,
                    minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp))
            } else {
                sectionContent(block)
            }
        }
    }

    pendingSectionRemoval?.let { selected ->
        AlertDialog(onDismissRequest = { pendingSectionRemoval = null },
            title = { Text("Remover ${selected.title}?") },
            text = { Text("Esta seção será retirada da estrutura do plano. Revise o conteúdo antes de salvar; os demais campos da aula não serão apagados.") },
            confirmButton = { TextButton(onClick = {
                pendingSectionRemoval = null
                onChange(layout.remove(selected.id))
            }, enabled = enabled) { Text("Remover seção") } },
            dismissButton = { TextButton(onClick = { pendingSectionRemoval = null }) { Text("Cancelar") } })
    }
    pendingRename?.let { selected ->
        AlertDialog(onDismissRequest = { pendingRename = null },
            title = { Text("Renomear seção") },
            text = {
                OutlinedTextField(renameTitle, { renameTitle = it.take(100) },
                    label = { Text("Novo título da seção") },
                    supportingText = { Text("Use de 2 a 100 caracteres. O nome atual só muda ao confirmar.") },
                    isError = renameTitle.trim().length < 2,
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = { TextButton(onClick = {
                pendingRename = null
                onChange(layout.update(selected.copy(title = renameTitle.trim())))
            }, enabled = enabled && renameTitle.trim().length in 2..100) { Text("Salvar nome") } },
            dismissButton = { TextButton(onClick = { pendingRename = null }) { Text("Cancelar") } })
    }
    pendingTemplate?.let { selected ->
        AlertDialog(onDismissRequest = { pendingTemplate = null },
            title = { Text("Aplicar modelo ${selected.second.name}?") },
            text = { Text("A estrutura atual e o texto de seções personalizadas serão substituídos. O título, objetivo, conteúdo e demais campos da aula permanecem armazenados.") },
            confirmButton = { TextButton(onClick = {
                pendingTemplate = null
                onChange(selected.second.instantiate())
            }) { Text("Aplicar modelo") } },
            dismissButton = { TextButton(onClick = { pendingTemplate = null }) { Text("Cancelar") } })
    }
    pendingRemoval?.let { id ->
        AlertDialog(onDismissRequest = { pendingRemoval = null },
            title = { Text("Excluir este modelo?") },
            text = { Text("Planos que já usam este modelo não serão modificados.") },
            confirmButton = { TextButton(onClick = { pendingRemoval = null; onRemoveTemplate(id) }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancelar") } })
    }
}
